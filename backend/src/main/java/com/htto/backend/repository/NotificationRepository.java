package com.htto.backend.repository;

import com.htto.backend.domain.DomainEnums.NotificationStatus;
import com.htto.backend.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientUserId(String recipientUserId);

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);

    List<Notification> findByRecipientUserIdAndStatus(String recipientUserId, NotificationStatus status);

    List<Notification> findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
            String recipientUserId,
            NotificationStatus status
    );

    Optional<Notification> findByIdAndRecipientUserId(String id, String recipientUserId);
}
