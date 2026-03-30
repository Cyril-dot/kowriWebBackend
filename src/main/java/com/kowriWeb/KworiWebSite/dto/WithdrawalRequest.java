// ─────────────────────────────────────────────────────────────────────
// File: dto/WithdrawalRequest.java
// ─────────────────────────────────────────────────────────────────────
package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.AgentService;
import com.kowriWeb.KworiWebSite.entity.NetworkProvider;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalRequest {
    private String accountName;
    private AgentService agentService;       // MOMO_AGENT or BANK_TRANSFER
    private NetworkProvider networkProvider; // MTN, VODAFONE, AIRTEL, TIGO
    private String accountNumber;
    private BigDecimal amount;               // minimum GH₵ 1700
}


