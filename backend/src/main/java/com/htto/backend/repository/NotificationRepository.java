package com.htto.backend.repository;

import com.htto.backend.domain.DomainEnums.NotificationStatus;
import com.htto.backend.domain.Notification;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientUserId(String recipientUserId);

    List<Notification> findByRecipientUserIdAndStatus(String recipientUserId, NotificationStatus status);
}
