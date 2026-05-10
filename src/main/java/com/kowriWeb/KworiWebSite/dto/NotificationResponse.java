
// ═══════════════════════════════════════════════════════════
// FILE: dto/NotificationResponse.java
// ═══════════════════════════════════════════════════════════
package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String message;
    private String imageUrl;
    private String sentByName;   // admin's fullName
    private LocalDateTime createdAt;

    // Only populated in user-facing responses
    private boolean read;
    private LocalDateTime readAt;
}
