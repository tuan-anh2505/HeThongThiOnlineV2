package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.NotificationStatus;
import java.time.Instant;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@CompoundIndex(name = "idx_notification_recipient_status", def = "{'recipientUserId': 1, 'status': 1}")
public class Notification extends AuditableDocument {

    private String title;

    private String content;

    @Indexed
    private String recipientUserId;

    @Indexed
    private Role recipientRole;

    @Indexed
    private String recipientGroupId;

    private Instant createdAtForUser;

    private NotificationStatus status = NotificationStatus.UNREAD;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public Role getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(Role recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getRecipientGroupId() {
        return recipientGroupId;
    }

    public void setRecipientGroupId(String recipientGroupId) {
        this.recipientGroupId = recipientGroupId;
    }

    public Instant getCreatedAtForUser() {
        return createdAtForUser;
    }

    public void setCreatedAtForUser(Instant createdAtForUser) {
        this.createdAtForUser = createdAtForUser;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }
}
