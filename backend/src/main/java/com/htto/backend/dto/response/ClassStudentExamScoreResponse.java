package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;

public record ClassStudentExamScoreResponse(
        String examId,
        String examName,
        String subjectId,
        String subjectName,
        ExamAttemptStatus latestStatus,
        int attemptCount,
        BigDecimal latestScore,
        BigDecimal bestScore
) {
}
