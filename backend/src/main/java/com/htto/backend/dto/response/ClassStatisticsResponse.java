package com.htto.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ClassStatisticsResponse(
        String classId,
        String classCode,
        String className,
        int studentCount,
        int examCount,
        BigDecimal classAverageScore,
        List<ClassStudentStatisticsResponse> students,
        List<ClassExamStatisticsResponse> exams,
        List<ScoreChartBucketResponse> scoreChart
) {
}
