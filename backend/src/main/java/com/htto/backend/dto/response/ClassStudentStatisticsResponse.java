package com.htto.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ClassStudentStatisticsResponse(
        String studentId,
        String studentCode,
        String fullName,
        String email,
        BigDecimal averageScore,
        List<ClassStudentExamScoreResponse> examScores
) {
}
