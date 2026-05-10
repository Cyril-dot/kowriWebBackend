// ═══════════════════════════════════════════════════════════
// FILE: repos/UserNotificationRepo.java
// ═══════════════════════════════════════════════════════════
package com.kowriWeb.KworiWebSite.entity.repos;

import com.kowriWeb.KworiWebSite.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationRepo extends JpaRepository<UserNotification, UUID> {

    /** All notifications for a user, newest first */
    List<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Unread count for a user */
    long countByUserIdAndReadFalse(UUID userId);

    /** Look up a specific user+notification pair */
    Optional<UserNotification> findByUserIdAndNotificationId(UUID userId, UUID notificationId);
}