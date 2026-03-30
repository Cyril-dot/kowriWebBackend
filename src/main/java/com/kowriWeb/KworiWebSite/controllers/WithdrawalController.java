package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.dto.WithdrawalRequest;
import com.kowriWeb.KworiWebSite.dto.WithdrawalResponse;
import com.kowriWeb.KworiWebSite.entity.WithdrawalStatus;
import com.kowriWeb.KworiWebSite.services.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;


    // ════════════════════════════════════════════════════════════════════
    // USER ENDPOINTS  —  /api/users/{userId}/withdrawals
    // ════════════════════════════════════════════════════════════════════

    /**
     * POST /api/users/{userId}/withdrawals
     * Submit a new withdrawal request.
     */
    @PostMapping("/api/users/{userId}/withdrawals")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<WithdrawalResponse> submitWithdrawal(
            @PathVariable UUID userId,
            @RequestBody WithdrawalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(withdrawalService.submitWithdrawal(userId, request));
    }

    /**
     * GET /api/users/{userId}/withdrawals
     * User views their own withdrawal history.
     */
    @GetMapping("/api/users/{userId}/withdrawals")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<WithdrawalResponse>> getUserWithdrawals(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(withdrawalService.getUserWithdrawals(userId));
    }

    /**
     * DELETE /api/users/{userId}/withdrawals/{withdrawalId}
     * User deletes one of their own withdrawals by ID.
     * Refunds balance automatically if the withdrawal was still PENDING.
     */
    @DeleteMapping("/api/users/{userId}/withdrawals/{withdrawalId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> userDeleteWithdrawal(
            @PathVariable UUID userId,
            @PathVariable UUID withdrawalId
    ) {
        withdrawalService.userDeleteWithdrawal(userId, withdrawalId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/users/{userId}/withdrawals
     * User clears their entire withdrawal history.
     * Refunds balance for any that were still PENDING.
     */
    @DeleteMapping("/api/users/{userId}/withdrawals")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> userDeleteAllWithdrawals(
            @PathVariable UUID userId
    ) {
        withdrawalService.userDeleteAllWithdrawals(userId);
        return ResponseEntity.noContent().build();
    }


    // ════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS  —  /api/admin/withdrawals
    // ════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/withdrawals
     * Admin views ALL withdrawal requests with owner info.
     */
    @GetMapping("/api/admin/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WithdrawalResponse>> getAllWithdrawals() {
        return ResponseEntity.ok(withdrawalService.getAllWithdrawals());
    }

    /**
     * GET /api/admin/withdrawals/{withdrawalId}
     * Admin views full details of a single withdrawal.
     */
    @GetMapping("/api/admin/withdrawals/{withdrawalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalResponse> getWithdrawalDetails(
            @PathVariable UUID withdrawalId
    ) {
        return ResponseEntity.ok(withdrawalService.getWithdrawalDetails(withdrawalId));
    }

    /**
     * PATCH /api/admin/withdrawals/{withdrawalId}/status?status=APPROVED
     * Admin approves or rejects a withdrawal.
     */
    @PatchMapping("/api/admin/withdrawals/{withdrawalId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalResponse> updateStatus(
            @PathVariable UUID withdrawalId,
            @RequestParam WithdrawalStatus status
    ) {
        return ResponseEntity.ok(withdrawalService.updateWithdrawalStatus(withdrawalId, status));
    }

    /**
     * DELETE /api/admin/withdrawals/{withdrawalId}
     * Admin deletes any single withdrawal by ID.
     * Refunds balance automatically if the withdrawal was still PENDING.
     */
    @DeleteMapping("/api/admin/withdrawals/{withdrawalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteWithdrawal(
            @PathVariable UUID withdrawalId
    ) {
        withdrawalService.adminDeleteWithdrawal(withdrawalId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/admin/withdrawals
     * Admin wipes the entire withdrawal history across all users.
     * Refunds balance for any that were still PENDING.
     */
    @DeleteMapping("/api/admin/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteAllWithdrawals() {
        withdrawalService.adminDeleteAllWithdrawals();
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/admin/withdrawals/users/{userId}
     * Admin deletes all withdrawals belonging to a specific user.
     * Refunds balance for any that were still PENDING.
     */
    @DeleteMapping("/api/admin/withdrawals/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteUserWithdrawals(
            @PathVariable UUID userId
    ) {
        withdrawalService.adminDeleteUserWithdrawals(userId);
        return ResponseEntity.noContent().build();
    }
}