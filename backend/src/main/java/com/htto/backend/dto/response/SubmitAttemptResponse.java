package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record SubmitAttemptResponse(
        boolean submitted,
        String attemptId,
        ExamAttemptStatus status,
        Instant submittedAt,
        boolean scoreVisible,
        BigDecimal totalScore
) {
}
