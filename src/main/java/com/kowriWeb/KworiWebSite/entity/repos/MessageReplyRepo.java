package com.kowriWeb.KworiWebSite.entity.repos;

import com.kowriWeb.KworiWebSite.entity.MessageReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageReplyRepo extends JpaRepository<MessageReply, UUID> {

    /** All replies for a given user-notification thread, oldest first. */
    List<MessageReply> findByUserNotificationIdOrderByCreatedAtAsc(UUID userNotificationId);
}