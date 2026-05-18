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


    // ════════════════════════════════════════════════════════════
    // ADMIN — BROADCAST (send to all users)
    // ════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/notifications/send
     * Broadcast a notification to ALL users.
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


    // ════════════════════════════════════════════════════════════
    // ADMIN — PRIVATE MESSAGE (send to one user)
    // ════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/notifications/private/{targetUserId}
     * Send a private message to a specific user only.
     *
     * Form fields:
     *   adminId  → UUID of the sending admin
     *   type     → TEXT_ONLY | IMAGE_ONLY | TEXT_IMAGE | PRIVATE_MESSAGE
     *   message  → required unless IMAGE_ONLY
     *   image    → required for IMAGE_ONLY and TEXT_IMAGE
     */
    @PostMapping(value = "/api/admin/notifications/private/{targetUserId}",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<NotificationResponse> sendPrivateMessage(
            @PathVariable UUID targetUserId,
            @RequestParam UUID adminId,
            @RequestParam NotificationType type,
            @RequestParam(required = false) String message,
            @RequestPart(required = false) MultipartFile image) throws IOException {

        SendNotificationRequest request = new SendNotificationRequest();
        request.setType(type);
        request.setMessage(message);
        request.setTargetUserId(targetUserId);

        return ResponseEntity.ok(
                notificationService.sendPrivateMessage(adminId, targetUserId, request, image));
    }


    // ════════════════════════════════════════════════════════════
    // ADMIN — VIEW ALL / DELETE
    // ════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/notifications
     * View all notifications ever sent (broadcast + private).
     */
    @GetMapping("/api/admin/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /**
     * DELETE /api/admin/notifications/{id}
     * Delete a notification and its Cloudinary image if any.
     */
    @DeleteMapping("/api/admin/notifications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) throws IOException {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }


    // ════════════════════════════════════════════════════════════
    // ADMIN — REPLY THREAD
    // ════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/threads/{userNotificationId}/replies
     * View the full reply thread for a user notification (admin view).
     */
    @GetMapping("/api/admin/threads/{userNotificationId}/replies")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MessageReplyResponse>> getThreadReplies(
            @PathVariable UUID userNotificationId) {
        return ResponseEntity.ok(notificationService.getThreadReplies(userNotificationId));
    }

    /**
     * POST /api/admin/threads/{userNotificationId}/reply
     * Admin replies to a user on a notification thread.
     *
     * Body (JSON):
     *   { "message": "..." }
     */
    @PostMapping("/api/admin/threads/{userNotificationId}/reply")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MessageReplyResponse> adminReply(
            @PathVariable UUID userNotificationId,
            @RequestParam UUID adminId,
            @RequestBody ReplyRequest request) {
        return ResponseEntity.ok(
                notificationService.adminReply(adminId, userNotificationId, request));
    }


    // ════════════════════════════════════════════════════════════
    // USER — GET NOTIFICATIONS
    // ════════════════════════════════════════════════════════════

    /**
     * GET /api/users/{userId}/notifications
     * Returns all notifications for the user (broadcast + private) with
     * their read status and full reply threads.
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


    // ════════════════════════════════════════════════════════════
    // USER — READ STATUS
    // ════════════════════════════════════════════════════════════

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


    // ════════════════════════════════════════════════════════════
    // USER — REPLY
    // ════════════════════════════════════════════════════════════

    /**
     * POST /api/users/{userId}/threads/{userNotificationId}/reply
     * User replies to a notification or private message thread.
     *
     * Body (JSON):
     *   { "message": "..." }
     *
     * Note: userNotificationId is returned in the NotificationResponse.userNotificationId field.
     */
    @PostMapping("/api/users/{userId}/threads/{userNotificationId}/reply")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MessageReplyResponse> userReply(
            @PathVariable UUID userId,
            @PathVariable UUID userNotificationId,
            @RequestBody ReplyRequest request) {
        return ResponseEntity.ok(
                notificationService.userReply(userId, userNotificationId, request));
    }

    /**
     * GET /api/users/{userId}/threads/{userNotificationId}/replies
     * User fetches all replies in a thread (to see their conversation with admin).
     */
    @GetMapping("/api/users/{userId}/threads/{userNotificationId}/replies")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MessageReplyResponse>> getUserThreadReplies(
            @PathVariable UUID userId,
            @PathVariable UUID userNotificationId) {
        // Re-use the same service method; ownership check is in userReply, not here.
        // For a read-only fetch we just verify the thread exists.
        return ResponseEntity.ok(notificationService.getThreadReplies(userNotificationId));
    }
}