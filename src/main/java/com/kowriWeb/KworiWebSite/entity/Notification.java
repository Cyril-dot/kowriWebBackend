package com.kowriWeb.KworiWebSite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String imageUrl;
    private String imagePublicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by")
    private User sentBy;

    /**
     * True  → private message (only targetUser receives it).
     * False → broadcast to all users.
     */
    @Column(nullable = false)
    private boolean privateMessage;

    /**
     * Non-null only when privateMessage = true.
     * Identifies which user this message was addressed to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}