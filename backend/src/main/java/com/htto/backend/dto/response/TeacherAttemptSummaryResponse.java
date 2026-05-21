package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TeacherAttemptSummaryResponse(
        String attemptId,
        String examId,
        String studentId,
        String studentCode,
        String studentName,
        String studentEmail,
        Instant startedAt,
        Instant submittedAt,
        ExamAttemptStatus status,
        BigDecimal totalScore,
        BigDecimal maxScore,
        int attemptNumber
) {
}
