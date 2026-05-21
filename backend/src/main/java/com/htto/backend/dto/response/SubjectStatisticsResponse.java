package com.htto.backend.dto.response;

import java.util.List;

public record SubjectStatisticsResponse(
        String subjectId,
        String subjectCode,
        String subjectName,
        int examCount,
        List<SubjectExamStatisticsResponse> exams
) {
}
