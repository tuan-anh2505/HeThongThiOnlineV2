package com.htto.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentAttemptResultResponse(
        String attemptId,
        String examId,
        String examName,
        String subjectName,
        int attemptNumber,
        ExamAttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        boolean scoreVisible,
        BigDecimal totalScore,
        BigDecimal maxScore,
        boolean reviewAllowed,
        boolean correctAnswerVisible
) {
}
