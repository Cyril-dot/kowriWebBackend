package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.entity.*;
import com.kowriWeb.KworiWebSite.entity.repos.MessageReplyRepo;
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
import java.util.Collections;
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
    private final MessageReplyRepo       messageReplyRepo;
    private final UserRepo               userRepo;
    private final CloudinaryService      cloudinaryService;

    private static final String NOTIFICATION_FOLDER = "kowri/notifications";


    // ──────────────────────────────────────────────────────────────
    // ADMIN: Send a broadcast notification to ALL users
    // ──────────────────────────────────────────────────────────────

    @Transactional
    public NotificationResponse sendNotification(UUID adminId,
                                                  SendNotificationRequest request,
                                                  MultipartFile image) throws IOException {

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        validateRequest(request, image);

        String imageUrl      = uploadImageIfPresent(image);
        String imagePublicId = null;

        if (image != null && !image.isEmpty()) {
            Map uploadResult = cloudinaryService.uploadImage(image, NOTIFICATION_FOLDER);
            imageUrl      = (String) uploadResult.get("secure_url");
            imagePublicId = (String) uploadResult.get("public_id");
        }

        Notification notification = Notification.builder()
                .type(request.getType())
                .message(request.getMessage())
                .imageUrl(imageUrl)
                .imagePublicId(imagePublicId)
                .sentBy(admin)
                .privateMessage(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepo.save(notification);

        // Fan out to ALL users
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

        log.info("Broadcast notification {} sent by admin {} to {} users",
                saved.getId(), admin.getEmail(), allUsers.size());

        return toResponse(saved, false, null, null, null);
    }


    // ──────────────────────────────────────────────────────────────
    // ADMIN: Send a PRIVATE message to a single user
    // ──────────────────────────────────────────────────────────────

    @Transactional
    public NotificationResponse sendPrivateMessage(UUID adminId,
                                                    UUID targetUserId,
                                                    SendNotificationRequest request,
                                                    MultipartFile image) throws IOException {

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        validateRequest(request, image);

        String imageUrl      = null;
        String imagePublicId = null;

        if (image != null && !image.isEmpty()) {
            Map uploadResult = cloudinaryService.uploadImage(image, NOTIFICATION_FOLDER);
            imageUrl      = (String) uploadResult.get("secure_url");
            imagePublicId = (String) uploadResult.get("public_id");
            log.info("Private message image uploaded: {}", imageUrl);
        }

        Notification notification = Notification.builder()
                .type(request.getType())
                .message(request.getMessage())
                .imageUrl(imageUrl)
                .imagePublicId(imagePublicId)
                .sentBy(admin)
                .privateMessage(true)
                .targetUser(targetUser)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepo.save(notification);

        // Only one UserNotification row — for the target user
        UserNotification un = UserNotification.builder()
                .user(targetUser)
                .notification(saved)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        UserNotification savedUn = userNotificationRepo.save(un);

        log.info("Private message {} sent by admin {} to user {}",
                saved.getId(), admin.getEmail(), targetUser.getEmail());

        return toResponse(saved, false, null, savedUn.getId(), Collections.emptyList());
    }


    // ──────────────────────────────────────────────────────────────
    // ADMIN: View all sent notifications (broadcast + private)
    // ──────────────────────────────────────────────────────────────

    public List<NotificationResponse> getAllNotifications() {
        return notificationRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(n -> {
                    UUID unId = null;
                    if (n.isPrivateMessage()) {
                        List<UserNotification> uns = userNotificationRepo.findByNotificationId(n.getId());
                        if (!uns.isEmpty()) {
                            unId = uns.get(0).getId();
                            log.info("Admin notifications list — notif {} mapped to userNotificationId {}", n.getId(), unId);
                        } else {
                            log.warn("Admin notifications list — private notif {} has no UserNotification row", n.getId());
                        }
                    }
                    return toResponse(n, false, null, unId, null);
                })
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────
    // ADMIN: View the full reply thread for a private message
    //        (identified by the UserNotification id)
    // ──────────────────────────────────────────────────────────────

    public List<MessageReplyResponse> getThreadReplies(UUID userNotificationId) {
        return messageReplyRepo
                .findByUserNotificationIdOrderByCreatedAtAsc(userNotificationId)
                .stream()
                .map(this::toReplyResponse)
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────
    // ADMIN: Reply to a user's message on a thread
    // ──────────────────────────────────────────────────────────────

    @Transactional
    public MessageReplyResponse adminReply(UUID adminId,
                                            UUID userNotificationId,
                                            ReplyRequest request) {

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        UserNotification un = userNotificationRepo.findById(userNotificationId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new RuntimeException("Reply message cannot be empty");
        }

        MessageReply reply = MessageReply.builder()
                .sender(admin)
                .userNotification(un)
                .message(request.getMessage())
                .fromAdmin(true)
                .createdAt(LocalDateTime.now())
                .build();

        MessageReply saved = messageReplyRepo.save(reply);

        // Mark the user's notification as unread again so they notice the admin replied
        un.setRead(false);
        un.setReadAt(null);
        userNotificationRepo.save(un);

        log.info("Admin {} replied to thread {}", admin.getEmail(), userNotificationId);
        return toReplyResponse(saved);
    }


    // ──────────────────────────────────────────────────────────────
    // ADMIN: Delete a notification (broadcast or private)
    // ──────────────────────────────────────────────────────────────

    @Transactional
    public void deleteNotification(UUID notificationId) throws IOException {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getImagePublicId() != null) {
            cloudinaryService.deleteImage(notification.getImagePublicId());
            log.info("Deleted notification image: {}", notification.getImagePublicId());
        }

        // Manually remove UserNotification rows (and their replies via cascade)
        List<UserNotification> refs = userNotificationRepo.findByNotificationId(notificationId);
        userNotificationRepo.deleteAll(refs);

        notificationRepo.delete(notification);
        log.info("Admin deleted notification {}", notificationId);
    }


    // ──────────────────────────────────────────────────────────────
    // USER: Get own notifications (broadcast + private, with threads)
    // ──────────────────────────────────────────────────────────────

    public List<NotificationResponse> getUserNotifications(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userNotificationRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(un -> {
                    List<MessageReplyResponse> replies = messageReplyRepo
                            .findByUserNotificationIdOrderByCreatedAtAsc(un.getId())
                            .stream()
                            .map(this::toReplyResponse)
                            .collect(Collectors.toList());

                    return toResponse(
                            un.getNotification(),
                            un.isRead(),
                            un.getReadAt(),
                            un.getId(),
                            replies
                    );
                })
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────────────────────────
    // USER: Get unread count
    // ──────────────────────────────────────────────────────────────

    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = userNotificationRepo.countByUserIdAndReadFalse(userId);
        log.info("Unread notifications for user {}: {}", userId, count);
        return new UnreadCountResponse(count);
    }


    // ──────────────────────────────────────────────────────────────
    // USER: Mark a single notification as read
    // ──────────────────────────────────────────────────────────────

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

        List<MessageReplyResponse> replies = messageReplyRepo
                .findByUserNotificationIdOrderByCreatedAtAsc(un.getId())
                .stream()
                .map(this::toReplyResponse)
                .collect(Collectors.toList());

        return toResponse(un.getNotification(), un.isRead(), un.getReadAt(), un.getId(), replies);
    }


    // ──────────────────────────────────────────────────────────────
    // USER: Mark ALL notifications as read
    // ──────────────────────────────────────────────────────────────

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


    // ──────────────────────────────────────────────────────────────
    // USER: Reply to a notification / private message
    // ──────────────────────────────────────────────────────────────

    @Transactional
    public MessageReplyResponse userReply(UUID userId,
                                           UUID userNotificationId,
                                           ReplyRequest request) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserNotification un = userNotificationRepo.findById(userNotificationId)
                .orElseThrow(() -> new RuntimeException("Notification thread not found"));

        // Make sure this thread actually belongs to this user
        if (!un.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: this thread does not belong to you");
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new RuntimeException("Reply message cannot be empty");
        }

        MessageReply reply = MessageReply.builder()
                .sender(user)
                .userNotification(un)
                .message(request.getMessage())
                .fromAdmin(false)
                .createdAt(LocalDateTime.now())
                .build();

        MessageReply saved = messageReplyRepo.save(reply);

        log.info("User {} replied to thread {}", user.getEmail(), userNotificationId);
        return toReplyResponse(saved);
    }


    // ──────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────

    /** Convenience — only called in the broadcast path where image upload is done inline. */
    private String uploadImageIfPresent(MultipartFile image) {
        return null; // actual upload handled in calling method to also capture publicId
    }

    private void validateRequest(SendNotificationRequest request, MultipartFile image) {
        if (request.getType() == null) {
            throw new RuntimeException("type is required: TEXT_ONLY, IMAGE_ONLY, TEXT_IMAGE, or PRIVATE_MESSAGE");
        }

        switch (request.getType()) {
            case TEXT_ONLY, PRIVATE_MESSAGE -> {
                if (request.getMessage() == null || request.getMessage().isBlank()) {
                    throw new RuntimeException("message is required for " + request.getType() + " notifications");
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

    private NotificationResponse toResponse(Notification n,
                                              boolean read,
                                              LocalDateTime readAt,
                                              UUID userNotificationId,
                                              List<MessageReplyResponse> replies) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .message(n.getMessage())
                .imageUrl(n.getImageUrl())
                .sentByName(n.getSentBy() != null ? n.getSentBy().getFullName() : "System")
                .createdAt(n.getCreatedAt())
                .read(read)
                .readAt(readAt)
                .privateMessage(n.isPrivateMessage())
                .userNotificationId(userNotificationId)
                .replies(replies)
                .build();
    }

    private MessageReplyResponse toReplyResponse(MessageReply r) {
        return MessageReplyResponse.builder()
                .id(r.getId())
                .senderId(r.getSender().getId())
                .senderName(r.getSender().getFullName())
                .message(r.getMessage())
                .fromAdmin(r.isFromAdmin())
                .createdAt(r.getCreatedAt())
                .build();
    }
}