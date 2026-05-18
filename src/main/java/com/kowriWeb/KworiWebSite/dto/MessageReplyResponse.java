package com.kowriWeb.KworiWebSite.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageReplyResponse {
    private UUID id;
    private UUID senderId;
    private String senderName;
    private String message;
    private boolean fromAdmin;
    private LocalDateTime createdAt;
}