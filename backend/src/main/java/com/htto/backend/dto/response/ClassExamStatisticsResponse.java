package com.htto.backend.dto.response;

import java.math.BigDecimal;

public record ClassExamStatisticsResponse(
        String examId,
        String examName,
        String subjectId,
        String subjectName,
        int assignedStudentCount,
        int attemptedStudentCount,
        BigDecimal averageScore
) {
}
