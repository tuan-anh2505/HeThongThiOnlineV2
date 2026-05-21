package com.htto.backend.dto.response;

import com.htto.backend.domain.SystemLog;
import java.time.Instant;

public record SystemLogResponse(
        String logId,
        String accountId,
        String action,
        String targetType,
        String targetId,
        String description,
        String ipAddress,
        Instant createdAt
) {

    public static SystemLogResponse from(SystemLog log) {
        return new SystemLogResponse(
                log.getId(),
                log.getAccountId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDescription(),
                log.getIpAddress(),
                log.getCreatedAt() == null ? log.getOccurredAt() : log.getCreatedAt()
        );
    }
}
