package com.kowriWeb.KworiWebSite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * TEXT_ONLY  → message only, no image
     * IMAGE_ONLY → image only, no message
     * TEXT_IMAGE → both message and image
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** Optional — null when type = IMAGE_ONLY */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** Optional — null when type = TEXT_ONLY */
    private String imageUrl;

    /** Cloudinary public_id — needed to delete the image later */
    private String imagePublicId;

    /** The admin who sent this notification */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by")
    private User sentBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}