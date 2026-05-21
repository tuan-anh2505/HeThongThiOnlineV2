package com.htto.backend.dto.response;

import com.htto.backend.domain.Notification;
import com.htto.backend.domain.Role;
import java.time.Instant;

public record NotificationResponse(
        String notificationId,
        String title,
        String content,
        String receiverId,
        Role receiverRole,
        boolean isRead,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReceiverId(),
                notification.getReceiverRole(),
                notification.isRead(),
                notification.getCreatedAt() == null
                        ? notification.getCreatedAtForUser()
                        : notification.getCreatedAt()
        );
    }
}
