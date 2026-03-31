package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.dto.WithdrawalRequest;
import com.kowriWeb.KworiWebSite.dto.WithdrawalResponse;
import com.kowriWeb.KworiWebSite.entity.User;
import com.kowriWeb.KworiWebSite.entity.Withdrawal;
import com.kowriWeb.KworiWebSite.entity.WithdrawalStatus;
import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import com.kowriWeb.KworiWebSite.entity.repos.WithdrawalRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WithdrawalRepo withdrawalRepo;
    private final UserRepo userRepo;

    private static final BigDecimal MINIMUM_WITHDRAWAL = new BigDecimal("1700");


    // ──────────────────────────────────────────────────────────────────
    // USER: Submit a withdrawal request
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public WithdrawalResponse submitWithdrawal(UUID userId, WithdrawalRequest request) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Enforce minimum withdrawal amount
        if (request.getAmount() == null ||
                request.getAmount().compareTo(MINIMUM_WITHDRAWAL) < 0) {
            throw new RuntimeException(
                    "Minimum withdrawal amount is GH₵ " + MINIMUM_WITHDRAWAL.toPlainString());
        }

        // 2. Block if user already has a pending withdrawal (prevents race condition)
        boolean hasPendingWithdrawal = withdrawalRepo
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .anyMatch(w -> w.getStatus() == WithdrawalStatus.PENDING);
        if (hasPendingWithdrawal) {
            throw new RuntimeException(
                    "You already have a pending withdrawal. Please wait for it to be processed.");
        }

        // 3. Check user has sufficient balance — reject immediately if not
        if (user.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException(
                    "Insufficient balance. Your current balance is GH₵ "
                            + user.getBalance().toPlainString());
        }

        // 4. Reserve (deduct) the amount immediately so it can't be double-spent
        user.setBalance(user.getBalance().subtract(request.getAmount()));
        userRepo.save(user);

        log.info("Balance reserved for withdrawal: user={} -{} | new balance={}",
                userId, request.getAmount(), user.getBalance());

        Withdrawal withdrawal = Withdrawal.builder()
                .user(user)
                .accountName(request.getAccountName())
                .agentService(request.getAgentService())
                .networkProvider(request.getNetworkProvider())
                .accountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .status(WithdrawalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Withdrawal saved = withdrawalRepo.save(withdrawal);
        log.info("Withdrawal submitted by user {}: amount={}", userId, request.getAmount());

        return toResponse(saved, false);
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: View own withdrawal history
    // ──────────────────────────────────────────────────────────────────

    public List<WithdrawalResponse> getUserWithdrawals(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return withdrawalRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(w -> toResponse(w, false))
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: View ALL withdrawals across every user
    // ──────────────────────────────────────────────────────────────────

    public List<WithdrawalResponse> getAllWithdrawals() {
        log.info("Admin fetching all withdrawals");
        return withdrawalRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(w -> toResponse(w, true))
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: View single withdrawal details
    // ──────────────────────────────────────────────────────────────────

    public WithdrawalResponse getWithdrawalDetails(UUID withdrawalId) {
        Withdrawal withdrawal = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

        log.info("Admin fetching withdrawal details: {}", withdrawalId);
        return toResponse(withdrawal, true);
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Approve or reject a withdrawal
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public WithdrawalResponse updateWithdrawalStatus(UUID withdrawalId,
                                                     WithdrawalStatus newStatus) {
        Withdrawal withdrawal = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

        // Guard: ignore if already processed
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException(
                    "Withdrawal is already " + withdrawal.getStatus() + ". Cannot update again.");
        }

        // Guard: re-validate balance before approving — catches cases where the
        // withdrawal was submitted before a deposit was credited (balance went negative)
        if (newStatus == WithdrawalStatus.APPROVED) {
            User user = withdrawal.getUser();
            if (user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                // Balance is negative — refund and auto-reject instead of approving
                user.setBalance(user.getBalance().add(withdrawal.getAmount()));
                userRepo.save(user);
                withdrawal.setStatus(WithdrawalStatus.REJECTED);
                withdrawal.setUpdatedAt(LocalDateTime.now());
                withdrawalRepo.save(withdrawal);
                log.warn("Approval blocked — negative balance detected. Auto-rejected & refunded: user={} +{} | new balance={}",
                        user.getId(), withdrawal.getAmount(), user.getBalance());
                throw new RuntimeException(
                        "Cannot approve: user has insufficient balance. Withdrawal has been auto-rejected and the amount refunded.");
            }
        }

        withdrawal.setStatus(newStatus);
        withdrawal.setUpdatedAt(LocalDateTime.now());

        // ── On REJECTED: refund the reserved amount back to the user ──
        if (newStatus == WithdrawalStatus.REJECTED) {
            User user = withdrawal.getUser();
            user.setBalance(user.getBalance().add(withdrawal.getAmount()));
            userRepo.save(user);

            log.info("Withdrawal rejected — balance refunded: user={} +{} | new balance={}",
                    user.getId(), withdrawal.getAmount(), user.getBalance());
        }
        // APPROVED: amount was already deducted at submission, nothing more to do

        Withdrawal updated = withdrawalRepo.save(withdrawal);
        log.info("Withdrawal {} status updated to {}", withdrawalId, newStatus);

        return toResponse(updated, true);
    }


    // ──────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ──────────────────────────────────────────────────────────────────

    private WithdrawalResponse toResponse(Withdrawal w, boolean includeOwner) {
        WithdrawalResponse.WithdrawalResponseBuilder builder = WithdrawalResponse.builder()
                .id(w.getId())
                .accountName(w.getAccountName())
                .agentService(w.getAgentService())
                .networkProvider(w.getNetworkProvider())
                .accountNumber(w.getAccountNumber())
                .amount(w.getAmount())
                .status(w.getStatus())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt());

        if (includeOwner && w.getUser() != null) {
            builder.userId(w.getUser().getId())
                    .userFullName(w.getUser().getFullName())
                    .userEmail(w.getUser().getEmail())
                    .userBalance(w.getUser().getBalance());
        }

        return builder.build();
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: Delete own withdrawal by ID
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void userDeleteWithdrawal(UUID userId, UUID withdrawalId) {
        Withdrawal withdrawal = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

        if (!withdrawal.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied. This withdrawal does not belong to you.");
        }

        if (withdrawal.getStatus() == WithdrawalStatus.PENDING) {
            User user = withdrawal.getUser();
            user.setBalance(user.getBalance().add(withdrawal.getAmount()));
            userRepo.save(user);
            log.info("Pending withdrawal deleted — balance refunded: user={} +{} | new balance={}",
                    userId, withdrawal.getAmount(), user.getBalance());
        }

        withdrawalRepo.delete(withdrawal);
        log.info("User {} deleted their withdrawal {}", userId, withdrawalId);
    }


    // ──────────────────────────────────────────────────────────────────
    // USER: Delete ALL own withdrawals
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void userDeleteAllWithdrawals(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Withdrawal> withdrawals = withdrawalRepo.findByUserIdOrderByCreatedAtDesc(userId);

        for (Withdrawal w : withdrawals) {
            if (w.getStatus() == WithdrawalStatus.PENDING) {
                User user = w.getUser();
                user.setBalance(user.getBalance().add(w.getAmount()));
                userRepo.save(user);
                log.info("Refunded pending withdrawal {} for user {} | +{}",
                        w.getId(), userId, w.getAmount());
            }
        }

        withdrawalRepo.deleteAll(withdrawals);
        log.info("User {} deleted all their withdrawals ({} records)", userId, withdrawals.size());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Delete any withdrawal by ID
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void adminDeleteWithdrawal(UUID withdrawalId) {
        Withdrawal withdrawal = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

        if (withdrawal.getStatus() == WithdrawalStatus.PENDING) {
            User user = withdrawal.getUser();
            user.setBalance(user.getBalance().add(withdrawal.getAmount()));
            userRepo.save(user);
            log.info("Admin deleted pending withdrawal — balance refunded: user={} +{} | new balance={}",
                    user.getId(), withdrawal.getAmount(), user.getBalance());
        }

        withdrawalRepo.delete(withdrawal);
        log.info("Admin deleted withdrawal {}", withdrawalId);
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Delete ALL withdrawals (full history wipe)
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void adminDeleteAllWithdrawals() {
        List<Withdrawal> all = withdrawalRepo.findAllByOrderByCreatedAtDesc();

        for (Withdrawal w : all) {
            if (w.getStatus() == WithdrawalStatus.PENDING) {
                User user = w.getUser();
                user.setBalance(user.getBalance().add(w.getAmount()));
                userRepo.save(user);
                log.info("Refunded pending withdrawal {} during admin wipe | user={} +{}",
                        w.getId(), user.getId(), w.getAmount());
            }
        }

        withdrawalRepo.deleteAll(all);
        log.info("Admin wiped entire withdrawal history ({} records)", all.size());
    }


    // ──────────────────────────────────────────────────────────────────
    // ADMIN: Delete ALL withdrawals for a specific user
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void adminDeleteUserWithdrawals(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Withdrawal> withdrawals = withdrawalRepo.findByUserIdOrderByCreatedAtDesc(userId);

        for (Withdrawal w : withdrawals) {
            if (w.getStatus() == WithdrawalStatus.PENDING) {
                User user = w.getUser();
                user.setBalance(user.getBalance().add(w.getAmount()));
                userRepo.save(user);
                log.info("Refunded pending withdrawal {} during admin user-wipe | user={} +{}",
                        w.getId(), userId, w.getAmount());
            }
        }

        withdrawalRepo.deleteAll(withdrawals);
        log.info("Admin deleted all withdrawals for user {} ({} records)", userId, withdrawals.size());
    }
}