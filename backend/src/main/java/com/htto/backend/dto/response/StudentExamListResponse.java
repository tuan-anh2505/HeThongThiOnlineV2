package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import java.time.Instant;

public record StudentExamListResponse(
        String examId,
        String examName,
        String subjectName,
        String className,
        int durationMinutes,
        boolean hasPassword,
        Instant startTime,
        Instant endTime,
        ExamStatus status,
        ExamSessionStatus sessionStatus,
        String attemptStatus,
        int attemptCount,
        int maxAttempts
) {
}
