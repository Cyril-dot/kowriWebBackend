

// ─────────────────────────────────────────────────────────────────────
// File: dto/DepositResponse.java
// Returned to both user and admin
// ─────────────────────────────────────────────────────────────────────
package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.DepositStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DepositResponse {
    private UUID id;
    private String tokenId;
    private String secretCode;
    private String walletId;
    private BigDecimal amount;
    private String proofImageUrl;
    private DepositStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Owner info — populated only in admin views
    private UUID userId;
    private String userFullName;
    private String userEmail;

    private BigDecimal rewardAmount;
    private BigDecimal userBalance;   // shown in admin view
}