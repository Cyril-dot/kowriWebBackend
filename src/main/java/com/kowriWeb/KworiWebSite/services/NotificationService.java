package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.entity.*;
import com.kowriWeb.KworiWebSite.entity.repos.NotificationRepo;
import com.kowriWeb.KworiWebSite.entity.repos.UserNotificationRepo;
import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepo       notificationRepo;
    private final UserNotificationRepo   userNotificationRepo;
    private final UserRepo               userRepo;
    private final CloudinaryService      cloudinaryService;

    private static final String NOTIFICATION_FOLDER = "kowri/notifications";


    // ──────────────────────────────────────────
    // ADMIN: Send a notification to ALL users
    // ──────────────────────────────────────────

    @Transactional
    public NotificationResponse sendNotification(UUID adminId,
                                                  SendNotificationRequest request,
                                                  MultipartFile image) throws IOException {

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        validateRequest(request, image);

        // Upload image if present
        String imageUrl      = null;
        String imagePublicId = null;

        if (image != null && !image.isEmpty()) {
            Map uploadResult = cloudinaryService.uploadImage(image, NOTIFICATION_FOLDER);
            imageUrl      = (String) uploadResult.get("secure_url");
            imagePublicId = (String) uploadResult.get("public_id");
            log.info("Notification image uploaded: {}", imageUrl);
        }

        // Save the notification
        Notification notification = Notification.builder()
                .type(request.getType())
                .message(request.getMessage())
                .imageUrl(imageUrl)
                .imagePublicId(imagePublicId)
                .sentBy(admin)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepo.save(notification);

        // Fan out — create one UserNotification row per user
        List<User> allUsers = userRepo.findAll();
        List<UserNotification> userNotifications = allUsers.stream()
                .map(user -> UserNotification.builder()
                        .user(user)
                        .notification(saved)
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        userNotificationRepo.saveAll(userNotifications);

        log.info("Notification {} sent by admin {} to {} users",
                saved.getId(), admin.getEmail(), allUsers.size());

        return toResponse(saved, false, null);
    }


    // ──────────────────────────────────────────
    // ADMIN: View all sent notifications
    // ──────────────────────────────────────────

    public List<NotificationResponse> getAllNotifications() {
        log.info("Admin fetching all notifications");
        return notificationRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(n -> toResponse(n, false, null))
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────
    // ADMIN: Delete a notification
    // ──────────────────────────────────────────

    @Transactional
    public void deleteNotification(UUID notificationId) throws IOException {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Delete image from Cloudinary if one exists
        if (notification.getImagePublicId() != null) {
            cloudinaryService.deleteImage(notification.getImagePublicId());
            log.info("Deleted notification image: {}", notification.getImagePublicId());
        }

        // Cascades to UserNotification rows via the DB or we delete them manually
        List<UserNotification> userRefs = userNotificationRepo
                .findByUserIdOrderByCreatedAtDesc(null); // handled below
        notificationRepo.delete(notification);

        log.info("Admin deleted notification {}", notificationId);
    }


    // ──────────────────────────────────────────
    // USER: Get own notifications (with read status)
    // ──────────────────────────────────────────

    public List<NotificationResponse> getUserNotifications(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userNotificationRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(un -> toResponse(un.getNotification(), un.isRead(), un.getReadAt()))
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────
    // USER: Get unread count
    // ──────────────────────────────────────────

    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = userNotificationRepo.countByUserIdAndReadFalse(userId);
        log.info("Unread notifications for user {}: {}", userId, count);
        return new UnreadCountResponse(count);
    }


    // ──────────────────────────────────────────
    // USER: Mark a single notification as read
    // ──────────────────────────────────────────

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        UserNotification un = userNotificationRepo
                .findByUserIdAndNotificationId(userId, notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found for this user"));

        if (!un.isRead()) {
            un.setRead(true);
            un.setReadAt(LocalDateTime.now());
            userNotificationRepo.save(un);
            log.info("User {} marked notification {} as read", userId, notificationId);
        }

        return toResponse(un.getNotification(), un.isRead(), un.getReadAt());
    }


    // ──────────────────────────────────────────
    // USER: Mark ALL notifications as read
    // ──────────────────────────────────────────

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<UserNotification> unread = userNotificationRepo
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(un -> !un.isRead())
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        unread.forEach(un -> {
            un.setRead(true);
            un.setReadAt(now);
        });

        userNotificationRepo.saveAll(unread);
        log.info("User {} marked {} notifications as read", userId, unread.size());
    }


    // ──────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────

    private void validateRequest(SendNotificationRequest request, MultipartFile image) {
        if (request.getType() == null) {
            throw new RuntimeException("type is required: TEXT_ONLY, IMAGE_ONLY, or TEXT_IMAGE");
        }

        switch (request.getType()) {
            case TEXT_ONLY -> {
                if (request.getMessage() == null || request.getMessage().isBlank()) {
                    throw new RuntimeException("message is required for TEXT_ONLY notifications");
                }
            }
            case IMAGE_ONLY -> {
                if (image == null || image.isEmpty()) {
                    throw new RuntimeException("image is required for IMAGE_ONLY notifications");
                }
            }
            case TEXT_IMAGE -> {
                if (request.getMessage() == null || request.getMessage().isBlank()) {
                    throw new RuntimeException("message is required for TEXT_IMAGE notifications");
                }
                if (image == null || image.isEmpty()) {
                    throw new RuntimeException("image is required for TEXT_IMAGE notifications");
                }
            }
        }
    }

    private NotificationResponse toResponse(Notification n, boolean read, LocalDateTime readAt) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .message(n.getMessage())
                .imageUrl(n.getImageUrl())
                .sentByName(n.getSentBy() != null ? n.getSentBy().getFullName() : "System")
                .createdAt(n.getCreatedAt())
                .read(read)
                .readAt(readAt)
                .build();
    }
}