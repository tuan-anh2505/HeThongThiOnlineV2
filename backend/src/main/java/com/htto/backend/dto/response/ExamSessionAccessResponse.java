package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import java.time.Instant;

public record ExamSessionAccessResponse(
        boolean allowed,
        String reason,
        String examSessionId,
        String examId,
        Instant startTime,
        Instant endTime,
        ExamSessionStatus status,
        long remainingSeconds
) {
}
