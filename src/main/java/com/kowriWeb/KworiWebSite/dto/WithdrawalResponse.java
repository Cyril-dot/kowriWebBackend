// ─────────────────────────────────────────────────────────────────────
// File: dto/WithdrawalResponse.java
// ─────────────────────────────────────────────────────────────────────
package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.AgentService;
import com.kowriWeb.KworiWebSite.entity.NetworkProvider;
import com.kowriWeb.KworiWebSite.entity.WithdrawalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WithdrawalResponse {
    private UUID id;
    private String accountName;
    private AgentService agentService;
    private NetworkProvider networkProvider;
    private String accountNumber;
    private BigDecimal amount;
    private WithdrawalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Populated only in admin views
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private BigDecimal userBalance;   // shown in admin view
}