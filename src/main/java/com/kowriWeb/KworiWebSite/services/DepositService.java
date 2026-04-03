package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.entity.*;
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

    // ── Fixed tier map ────────────────────────────────────────────────
    private static final Map<BigDecimal, BigDecimal> DEPOSIT_TIERS = Map.of(
            new BigDecimal("300"),  new BigDecimal("3500"),
            new BigDecimal("500"),  new BigDecimal("5500"),
            new BigDecimal("1000"), new BigDecimal("10500")
    );

    private BigDecimal getFixedReward(BigDecimal amount) {
        if (amount == null) return null;
        for (Map.Entry<BigDecimal, BigDecimal> entry : DEPOSIT_TIERS.entrySet()) {
            if (entry.getKey().compareTo(amount) == 0) {
                return entry.getValue();
            }
        }
        return null;
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: Submit a deposit (FIXED or FLEXIBLE)
    // ──────────────────────────────────────────────────────────────────

    public DepositResponse submitDeposit(UUID userId,
                                         DepositRequest request,
                                         MultipartFile proofImage) throws IOException {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getDepositType() == null) {
            throw new RuntimeException("depositType is required: FIXED or FLEXIBLE");
        }

        BigDecimal reward = null;

        if (request.getDepositType() == DepositType.FIXED) {
            // Validate amount against fixed tiers
            reward = getFixedReward(request.getAmount());
            if (reward == null) {
                throw new RuntimeException(
                        "Invalid fixed deposit amount. Allowed tiers: GH₵ 300 → 3,500 | GH₵ 500 → 5,500 | GH₵ 1,000 → 10,500");
            }
        }
        // For FLEXIBLE: reward is null at submission — admin sets it on approval

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
                .rewardAmount(reward)           // null for FLEXIBLE until admin approves
                .depositType(request.getDepositType())
                .proofImageUrl(imageUrl)
                .proofImagePublicId(imagePublicId)
                .status(DepositStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Deposit saved = depositRepo.save(deposit);
        log.info("Deposit submitted by user {} | type={} | amount={} | reward={}",
                userId, request.getDepositType(), request.getAmount(), reward);

        return toResponse(saved, true);
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Approve or reject a deposit
    //   - FIXED:    reward already stored, just credit on approval
    //   - FLEXIBLE: admin must supply rewardAmount in the request
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public DepositResponse updateDepositStatus(UUID depositId, StatusUpdateRequest request) {
        Deposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw new RuntimeException(
                    "Deposit is already " + deposit.getStatus() + ". Cannot update again.");
        }

        // For FLEXIBLE approvals, admin must provide the reward amount
        if (request.getStatus() == DepositStatus.APPROVED
                && deposit.getDepositType() == DepositType.FLEXIBLE) {

            if (request.getRewardAmount() == null
                    || request.getRewardAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException(
                        "rewardAmount is required and must be greater than 0 when approving a FLEXIBLE deposit.");
            }
            deposit.setRewardAmount(request.getRewardAmount());
        }

        deposit.setStatus(request.getStatus());
        deposit.setUpdatedAt(LocalDateTime.now());

        // Credit balance only on APPROVED
        if (request.getStatus() == DepositStatus.APPROVED) {
            User user = deposit.getUser();
            BigDecimal reward = deposit.getRewardAmount();

            user.setBalance(user.getBalance().add(reward));
            userRepo.save(user);

            log.info("Balance credited: user={} +{} | new balance={}",
                    user.getId(), reward, user.getBalance());
        }

        Deposit updated = depositRepo.save(deposit);
        log.info("Deposit {} status updated to {}", depositId, request.getStatus());

        return toResponse(updated, true);
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Directly credit any amount to a user's balance
    //        Creates an ADMIN_CREDIT record for full history tracking
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public DepositResponse adminCreditUser(AdminCreditRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Credit amount must be greater than 0.");
        }

        User user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Credit balance immediately — no approval step needed
        user.setBalance(user.getBalance().add(request.getAmount()));
        userRepo.save(user);

        // Record the credit as a deposit entry for history
        Deposit record = Deposit.builder()
                .user(user)
                .tokenId("ADMIN_CREDIT")
                .secretCode(null)
                .walletId(null)
                .amount(request.getAmount())
                .rewardAmount(request.getAmount())
                .depositType(DepositType.ADMIN_CREDIT)
                .proofImageUrl(null)
                .proofImagePublicId(null)
                .status(DepositStatus.APPROVED)  // auto-approved instantly
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Deposit saved = depositRepo.save(record);
        log.info("Admin credited user {} with GH₵{} | new balance={}",
                user.getId(), request.getAmount(), user.getBalance());

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
    // ADMIN: View ALL transactions
    // ──────────────────────────────────────────────────────────────────

    public List<DepositResponse> getAllTransactions() {
        log.info("Admin fetching all transactions");
        return depositRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(d -> toResponse(d, true))
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: View single transaction details
    // ──────────────────────────────────────────────────────────────────

    public DepositResponse getTransactionDetails(UUID depositId) {
        Deposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        log.info("Admin fetching deposit details: {}", depositId);
        return toResponse(deposit, true);
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
    // ADMIN: Delete ALL deposits
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
                .depositType(deposit.getDepositType())
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
}