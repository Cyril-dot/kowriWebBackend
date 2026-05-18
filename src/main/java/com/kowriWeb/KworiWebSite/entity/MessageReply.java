package com.kowriWeb.KworiWebSite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores a reply written by a user (or admin) on a UserNotification thread.
 *
 * sender  → the person writing this reply (could be user or admin)
 * userNotification → the UserNotification record this reply belongs to
 *                    (ties reply to both the original Notification AND the specific user)
 */
@Entity
@Table(name = "message_replies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReply {

    @Id
    @GeneratedValue
    private UUID id;

    /** The person who wrote this reply (user or admin). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /** Which user-notification thread this reply belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_notification_id", nullable = false)
    private UserNotification userNotification;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** True when the reply was written by an admin (useful for frontend display). */
    @Column(nullable = false)
    private boolean fromAdmin;
}