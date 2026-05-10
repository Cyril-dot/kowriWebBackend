package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.entity.NotificationType;
import com.kowriWeb.KworiWebSite.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    // ════════════════════════════════════════════
    // ADMIN ENDPOINTS
    // ════════════════════════════════════════════

    /**
     * POST /api/admin/notifications/send
     * Accepts multipart/form-data so image can be attached.
     *
     * Form fields:
     *   adminId  → UUID of the sending admin
     *   type     → TEXT_ONLY | IMAGE_ONLY | TEXT_IMAGE
     *   message  → required for TEXT_ONLY and TEXT_IMAGE
     *   image    → required for IMAGE_ONLY and TEXT_IMAGE
     */
    @PostMapping(value = "/api/admin/notifications/send",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestParam UUID adminId,
            @RequestParam NotificationType type,
            @RequestParam(required = false) String message,
            @RequestPart(required = false) MultipartFile image) throws IOException {

        SendNotificationRequest request = new SendNotificationRequest();
        request.setType(type);
        request.setMessage(message);

        return ResponseEntity.ok(notificationService.sendNotification(adminId, request, image));
    }

    /**
     * GET /api/admin/notifications
     * View all notifications ever sent.
     */
    @GetMapping("/api/admin/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /**
     * DELETE /api/admin/notifications/{id}
     * Delete a notification (also removes its Cloudinary image if any).
     */
    @DeleteMapping("/api/admin/notifications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) throws IOException {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }


    // ════════════════════════════════════════════
    // USER ENDPOINTS
    // ════════════════════════════════════════════

    /**
     * GET /api/users/{userId}/notifications
     * Returns all notifications for the user with their read/unread status.
     */
    @GetMapping("/api/users/{userId}/notifications")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    /**
     * GET /api/users/{userId}/notifications/unread-count
     * Returns the number of unread notifications.
     */
    @GetMapping("/api/users/{userId}/notifications/unread-count")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    /**
     * PATCH /api/users/{userId}/notifications/{notificationId}/read
     * Mark a single notification as read.
     */
    @PatchMapping("/api/users/{userId}/notifications/{notificationId}/read")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID userId,
            @PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(userId, notificationId));
    }

    /**
     * PATCH /api/users/{userId}/notifications/read-all
     * Mark all notifications as read at once.
     */
    @PatchMapping("/api/users/{userId}/notifications/read-all")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}