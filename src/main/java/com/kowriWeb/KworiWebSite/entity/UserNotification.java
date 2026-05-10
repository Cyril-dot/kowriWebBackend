package com.kowriWeb.KworiWebSite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per (user, notification) pair.
 * Lets us track whether each user has read each notification.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "user_notifications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_id"}))
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Builder.Default
    private boolean read = false;

    private LocalDateTime readAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}