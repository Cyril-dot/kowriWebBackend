package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.dto.DepositRequest;
import com.kowriWeb.KworiWebSite.dto.DepositResponse;
import com.kowriWeb.KworiWebSite.entity.Deposit;
import com.kowriWeb.KworiWebSite.entity.DepositStatus;
import com.kowriWeb.KworiWebSite.entity.User;
import com.kowriWeb.KworiWebSite.entity.repos.DepositRepo;
import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositService {

    private final DepositRepo depositRepo;
    private final UserRepo userRepo;
    private final CloudinaryService cloudinaryService;

    private static final String CLOUDINARY_FOLDER = "kowri/deposit-proofs";

    // ── Tier map: deposited amount → reward credited to account ──────
    private static final Map<BigDecimal, BigDecimal> DEPOSIT_TIERS = Map.of(
            new BigDecimal("300"),  new BigDecimal("3500"),
            new BigDecimal("500"),  new BigDecimal("5500"),
            new BigDecimal("1000"), new BigDecimal("10500")
    );

    // ✅ FIX: Scale-insensitive lookup — 500, 500.0, 500.00 all match correctly
    private BigDecimal getReward(BigDecimal amount) {
        if (amount == null) return null;
        for (Map.Entry<BigDecimal, BigDecimal> entry : DEPOSIT_TIERS.entrySet()) {
            if (entry.getKey().compareTo(amount) == 0) {
                return entry.getValue();
            }
        }
        return null;
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: Submit a deposit with proof screenshot
    // ──────────────────────────────────────────────────────────────────

    public DepositResponse submitDeposit(UUID userId,
                                         DepositRequest request,
                                         MultipartFile proofImage) throws IOException {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ FIX: Use compareTo-based lookup instead of Map.get() (fixes BigDecimal scale mismatch)
        BigDecimal reward = getReward(request.getAmount());
        if (reward == null) {
            throw new RuntimeException(
                    "Invalid deposit amount. Allowed tiers: GH₵ 300 → 3,500 | GH₵ 500 → 5,500 | GH₵ 1,000 → 10,500");
        }

        // Upload proof screenshot to Cloudinary
        Map uploadResult = cloudinaryService.uploadImage(proofImage, CLOUDINARY_FOLDER);
        String imageUrl      = (String) uploadResult.get("secure_url");
        String imagePublicId = (String) uploadResult.get("public_id");

        log.info("Proof image uploaded for user {}: {}", userId, imageUrl);

        Deposit deposit = Deposit.builder()
                .user(user)
                .tokenId(request.getTokenId())
                .secretCode(request.getSecretCode())
                .walletId(request.getWalletId())
                .amount(request.getAmount())
                .rewardAmount(reward)
                .proofImageUrl(imageUrl)
                .proofImagePublicId(imagePublicId)
                .status(DepositStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Deposit saved = depositRepo.save(deposit);
        log.info("Deposit submitted by user {}: amount={}, reward={}", userId, request.getAmount(), reward);

        return toResponse(saved, true);
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: View own transactions
    // ──────────────────────────────────────────────────────────────────

    public List<DepositResponse> getUserTransactions(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return depositRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(d -> toResponse(d, false))
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: View ALL transactions (with owner info)
    // ──────────────────────────────────────────────────────────────────

    public List<DepositResponse> getAllTransactions() {
        log.info("Admin fetching all transactions");
        return depositRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(d -> toResponse(d, true))
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: View single transaction details (includes image + owner)
    // ──────────────────────────────────────────────────────────────────

    public DepositResponse getTransactionDetails(UUID depositId) {
        Deposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        log.info("Admin fetching deposit details: {}", depositId);
        return toResponse(deposit, true);
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Approve or reject a deposit
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public DepositResponse updateDepositStatus(UUID depositId, DepositStatus newStatus) {
        Deposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Guard: ignore if already processed to prevent double-crediting
        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw new RuntimeException(
                    "Deposit is already " + deposit.getStatus() + ". Cannot update again.");
        }

        deposit.setStatus(newStatus);
        deposit.setUpdatedAt(LocalDateTime.now());

        // ── Credit user's balance only on APPROVED ────────────────────
        if (newStatus == DepositStatus.APPROVED) {
            User user = deposit.getUser();
            BigDecimal reward = deposit.getRewardAmount();

            user.setBalance(user.getBalance().add(reward));
            userRepo.save(user);

            log.info("Balance credited: user={} +{} | new balance={}",
                    user.getId(), reward, user.getBalance());
        }

        Deposit updated = depositRepo.save(deposit);
        log.info("Deposit {} status updated to {}", depositId, newStatus);

        return toResponse(updated, true);
    }


    // ──────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ──────────────────────────────────────────────────────────────────

    private DepositResponse toResponse(Deposit deposit, boolean includeOwner) {
        DepositResponse.DepositResponseBuilder builder = DepositResponse.builder()
                .id(deposit.getId())
                .tokenId(deposit.getTokenId())
                .secretCode(deposit.getSecretCode())
                .walletId(deposit.getWalletId())
                .amount(deposit.getAmount())
                .rewardAmount(deposit.getRewardAmount())
                .proofImageUrl(deposit.getProofImageUrl())
                .status(deposit.getStatus())
                .createdAt(deposit.getCreatedAt())
                .updatedAt(deposit.getUpdatedAt());

        if (includeOwner && deposit.getUser() != null) {
            builder.userId(deposit.getUser().getId())
                    .userFullName(deposit.getUser().getFullName())
                    .userEmail(deposit.getUser().getEmail())
                    .userBalance(deposit.getUser().getBalance());
        }

        return builder.build();
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: Delete own deposit by ID
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void userDeleteDeposit(UUID userId, UUID depositId) {
        Deposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!deposit.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied. This transaction does not belong to you.");
        }

        depositRepo.delete(deposit);
        log.info("User {} deleted their deposit {}", userId, depositId);
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: Delete ALL own deposits
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void userDeleteAllDeposits(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Deposit> deposits = depositRepo.findByUserIdOrderByCreatedAtDesc(userId);
        depositRepo.deleteAll(deposits);
        log.info("User {} deleted all their deposits ({} records)", userId, deposits.size());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Delete any deposit by ID
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void adminDeleteDeposit(UUID depositId) {
        Deposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        depositRepo.delete(deposit);
        log.info("Admin deleted deposit {}", depositId);
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Delete ALL deposits (full history wipe)
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void adminDeleteAllDeposits() {
        long count = depositRepo.count();
        depositRepo.deleteAll();
        log.info("Admin wiped entire deposit history ({} records)", count);
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Delete ALL deposits for a specific user
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void adminDeleteUserDeposits(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Deposit> deposits = depositRepo.findByUserIdOrderByCreatedAtDesc(userId);
        depositRepo.deleteAll(deposits);
        log.info("Admin deleted all deposits for user {} ({} records)", userId, deposits.size());
    }
}