package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.dto.DepositRequest;
import com.kowriWeb.KworiWebSite.dto.DepositResponse;
import com.kowriWeb.KworiWebSite.entity.DepositStatus;
import com.kowriWeb.KworiWebSite.services.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;


    // ════════════════════════════════════════════════════════════════════
    // USER ENDPOINTS  —  /api/users/{userId}/deposits
    // ════════════════════════════════════════════════════════════════════

    /**
     * POST /api/users/{userId}/deposits
     * Submit a new deposit with proof screenshot.
     */
    @PostMapping(
            value = "/api/users/{userId}/deposits",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DepositResponse> submitDeposit(
            @PathVariable UUID userId,
            @RequestPart("data")  DepositRequest request,
            @RequestPart("proof") MultipartFile proofImage
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(depositService.submitDeposit(userId, request, proofImage));
    }

    /**
     * GET /api/users/{userId}/deposits
     * User views their own transaction history.
     */
    @GetMapping("/api/users/{userId}/deposits")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<DepositResponse>> getUserTransactions(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(depositService.getUserTransactions(userId));
    }

    /**
     * DELETE /api/users/{userId}/deposits/{depositId}
     * User deletes one of their own deposits by ID.
     */
    @DeleteMapping("/api/users/{userId}/deposits/{depositId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> userDeleteDeposit(
            @PathVariable UUID userId,
            @PathVariable UUID depositId
    ) {
        depositService.userDeleteDeposit(userId, depositId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/users/{userId}/deposits
     * User clears their entire deposit history.
     */
    @DeleteMapping("/api/users/{userId}/deposits")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> userDeleteAllDeposits(
            @PathVariable UUID userId
    ) {
        depositService.userDeleteAllDeposits(userId);
        return ResponseEntity.noContent().build();
    }


    // ════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS  —  /api/admin/deposits
    // ════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/deposits
     * Admin views ALL transactions across every user.
     */
    @GetMapping("/api/admin/deposits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DepositResponse>> getAllTransactions() {
        return ResponseEntity.ok(depositService.getAllTransactions());
    }

    /**
     * GET /api/admin/deposits/{depositId}
     * Admin views full details of a single transaction (includes proof image URL + owner).
     */
    @GetMapping("/api/admin/deposits/{depositId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepositResponse> getTransactionDetails(
            @PathVariable UUID depositId
    ) {
        return ResponseEntity.ok(depositService.getTransactionDetails(depositId));
    }

    /**
     * PATCH /api/admin/deposits/{depositId}/status?status=APPROVED
     * Admin approves or rejects a deposit.
     */
    @PatchMapping("/api/admin/deposits/{depositId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepositResponse> updateStatus(
            @PathVariable UUID depositId,
            @RequestParam DepositStatus status
    ) {
        return ResponseEntity.ok(depositService.updateDepositStatus(depositId, status));
    }

    /**
     * DELETE /api/admin/deposits/{depositId}
     * Admin deletes any single deposit by ID.
     */
    @DeleteMapping("/api/admin/deposits/{depositId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteDeposit(
            @PathVariable UUID depositId
    ) {
        depositService.adminDeleteDeposit(depositId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/admin/deposits
     * Admin wipes the entire deposit history across all users.
     */
    @DeleteMapping("/api/admin/deposits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteAllDeposits() {
        depositService.adminDeleteAllDeposits();
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/admin/deposits/users/{userId}
     * Admin deletes all deposits belonging to a specific user.
     */
    @DeleteMapping("/api/admin/deposits/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteUserDeposits(
            @PathVariable UUID userId
    ) {
        depositService.adminDeleteUserDeposits(userId);
        return ResponseEntity.noContent().build();
    }
}