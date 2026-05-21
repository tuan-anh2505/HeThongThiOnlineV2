package com.htto.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExamStatisticsResponse(
        String examId,
        String examName,
        String subjectId,
        String subjectName,
        BigDecimal maxScore,
        int assignedStudentCount,
        int attemptedStudentCount,
        int notStartedStudentCount,
        int submittedAttemptCount,
        int expiredAttemptCount,
        BigDecimal highestScore,
        BigDecimal lowestScore,
        BigDecimal averageScore,
        List<ExamStudentScoreStatisticResponse> studentScores
) {
}
