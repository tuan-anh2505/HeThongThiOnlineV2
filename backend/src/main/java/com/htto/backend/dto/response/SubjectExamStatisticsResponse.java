package com.htto.backend.dto.response;

import java.math.BigDecimal;

public record SubjectExamStatisticsResponse(
        String examId,
        String examName,
        int assignedStudentCount,
        int attemptedStudentCount,
        BigDecimal averageScore
) {
}
