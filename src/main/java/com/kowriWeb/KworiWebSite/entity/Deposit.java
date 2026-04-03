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
@Table(name = "deposit_table")
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // ── Deposit form fields ────────────────────────────────────────────
    private String tokenId;
    private String secretCode;
    private String walletId;

    @Column(nullable = false)
    private BigDecimal amount;

    // ── Cloudinary proof image ─────────────────────────────────────────
    private String proofImageUrl;      // full https URL
    private String proofImagePublicId; // cloudinary public_id (for deletion)

    // ── Status ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DepositStatus status = DepositStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private DepositType depositType;

    // ── Timestamps ─────────────────────────────────────────────────────
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // ── Owner ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = true, precision = 12, scale = 2)
    private BigDecimal rewardAmount;
}