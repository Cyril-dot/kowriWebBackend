package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String message;
    private String imageUrl;
    private String sentByName;
    private LocalDateTime createdAt;
    private boolean read;
    private LocalDateTime readAt;

    /** True when this notification was sent privately to one specific user. */
    private boolean privateMessage;

    /** Populated when fetching a user's own notifications (includes full reply thread). */
    private List<MessageReplyResponse> replies;

    /** The user_notification record ID — needed by the reply endpoints. */
    private UUID userNotificationId;
}