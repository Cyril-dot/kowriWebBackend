package com.kowriWeb.KworiWebSite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "withdrawal_table")
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // ── Withdrawal form fields ─────────────────────────────────────────
    private String accountName;

    @Enumerated(EnumType.STRING)
    private AgentService agentService;   // MOMO_AGENT, BANK_TRANSFER

    @Enumerated(EnumType.STRING)
    private NetworkProvider networkProvider; // MTN, VODAFONE, AIRTEL, TIGO

    private String accountNumber;        // phone or bank account number

    @Column(nullable = false)
    private BigDecimal amount;           // minimum GH₵ 1700

    // ── Status ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    // ── Timestamps ─────────────────────────────────────────────────────
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // ── Owner ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}