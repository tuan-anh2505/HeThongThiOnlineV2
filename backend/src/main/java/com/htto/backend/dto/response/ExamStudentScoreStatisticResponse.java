package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ExamStudentScoreStatisticResponse(
        String studentId,
        String studentCode,
        String fullName,
        String email,
        int attemptCount,
        int submittedAttemptCount,
        int expiredAttemptCount,
        ExamAttemptStatus latestStatus,
        Instant latestSubmittedAt,
        BigDecimal latestScore,
        BigDecimal bestScore,
        List<StatisticsAttemptScoreResponse> attempts
) {
}
