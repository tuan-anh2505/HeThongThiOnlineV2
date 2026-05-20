package com.htto.backend.dto.request;

import java.util.List;

public record ExamUpdateRequest(
        String examName,
        String classId,
        List<String> classIds,
        String subjectId,
        String questionBankId,
        String teacherId,
        Integer durationMinutes,
        Integer maxAttempts,
        Boolean allowViewScore,
        Boolean allowReview,
        Boolean allowViewCorrectAnswer,
        Boolean shuffleQuestions,
        Boolean shuffleOptions
) {
}
