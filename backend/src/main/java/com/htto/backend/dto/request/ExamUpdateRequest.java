package com.htto.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ExamUpdateRequest(
        String examName,
        String classId,
        List<@NotBlank String> classIds,
        String subjectId,
        String questionBankId,
        String teacherId,
        @Min(1)
        Integer durationMinutes,
        @Min(1)
        Integer maxAttempts,
        Boolean allowViewScore,
        Boolean allowReview,
        Boolean allowViewCorrectAnswer,
        Boolean shuffleQuestions,
        Boolean shuffleOptions,
        String examPassword,
        Boolean removePassword
) {
}
