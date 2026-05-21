package com.htto.backend.domain;

import java.time.Instant;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "system_logs")
@CompoundIndex(name = "idx_log_account_time", def = "{'accountId': 1, 'createdAt': -1}")
public class SystemLog extends AuditableDocument {

    @Indexed
    private String accountId;

    @Indexed
    private String action;

    @Indexed
    private Instant occurredAt;

    private String ipAddress;

    @Indexed
    private String targetType;

    @Indexed
    private String targetId;

    private String description;

    public String getLogId() {
        return getId();
    }

    public void setLogId(String logId) {
        setId(logId);
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return accountId;
    }

    public void setUserId(String userId) {
        this.accountId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetail() {
        return description;
    }

    public void setDetail(String detail) {
        this.description = detail;
    }
}
