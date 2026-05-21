package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public record ExamAttemptStatusResponse(
        String attemptId,
        String examId,
        ExamAttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        Instant deadline,
        long remainingSeconds,
        boolean expired,
        boolean scoreVisible,
        BigDecimal totalScore
) {

    public static ExamAttemptStatusResponse of(
            String attemptId,
            String examId,
            ExamAttemptStatus status,
            Instant startedAt,
            Instant submittedAt,
            Instant deadline,
            Instant now,
            boolean scoreVisible,
            BigDecimal totalScore
    ) {
        long remainingSeconds = deadline == null ? 0 : Math.max(0, Duration.between(now, deadline).getSeconds());
        boolean expired = status == ExamAttemptStatus.EXPIRED
                || (status == ExamAttemptStatus.IN_PROGRESS && deadline != null && !now.isBefore(deadline));
        return new ExamAttemptStatusResponse(
                attemptId,
                examId,
                status,
                startedAt,
                submittedAt,
                deadline,
                remainingSeconds,
                expired,
                scoreVisible,
                scoreVisible ? totalScore : null
        );
    }
}
