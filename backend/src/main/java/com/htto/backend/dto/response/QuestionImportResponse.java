package com.htto.backend.dto.response;

import java.util.List;

public record QuestionImportResponse(
        int totalQuestions,
        int importedCount,
        boolean success,
        List<QuestionImportErrorResponse> errors,
        List<QuestionResponse> questions
) {

    public static QuestionImportResponse failed(int totalQuestions, List<QuestionImportErrorResponse> errors) {
        return new QuestionImportResponse(totalQuestions, 0, false, errors, List.of());
    }

    public static QuestionImportResponse success(List<QuestionResponse> questions) {
        return new QuestionImportResponse(questions.size(), questions.size(), true, List.of(), questions);
    }
}
