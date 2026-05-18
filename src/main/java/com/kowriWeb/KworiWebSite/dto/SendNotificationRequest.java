package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.NotificationType;
import lombok.Data;

import java.util.UUID;

@Data
public class SendNotificationRequest {

    private NotificationType type;
    private String message;

    /**
     * When set, the notification is sent ONLY to this user (private message).
     * When null, it is broadcast to ALL users.
     */
    private UUID targetUserId;
}