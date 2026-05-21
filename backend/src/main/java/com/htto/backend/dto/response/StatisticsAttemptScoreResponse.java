package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record StatisticsAttemptScoreResponse(
        String attemptId,
        int attemptNumber,
        ExamAttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal totalScore
) {
}
