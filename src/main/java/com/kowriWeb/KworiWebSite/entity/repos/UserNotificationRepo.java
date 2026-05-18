package com.kowriWeb.KworiWebSite.entity.repos;

import com.kowriWeb.KworiWebSite.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepo extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserNotification> findByUserIdAndNotificationId(UUID userId, UUID notificationId);

    long countByUserIdAndReadFalse(UUID userId);

    /** All UserNotification rows for a given notification (used for broadcast deletes). */
    List<UserNotification> findByNotificationId(UUID notificationId);
}