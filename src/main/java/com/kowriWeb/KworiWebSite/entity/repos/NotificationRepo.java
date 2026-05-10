// ═══════════════════════════════════════════════════════════
// FILE: repos/NotificationRepo.java
// ═══════════════════════════════════════════════════════════
package com.kowriWeb.KworiWebSite.entity.repos;

import com.kowriWeb.KworiWebSite.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByOrderByCreatedAtDesc();
}


