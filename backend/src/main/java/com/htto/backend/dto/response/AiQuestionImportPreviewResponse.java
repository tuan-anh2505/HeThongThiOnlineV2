package com.htto.backend.dto.response;

import java.util.List;

public record AiQuestionImportPreviewResponse(
        boolean success,
        int totalQuestions,
        int validCount,
        int failedCount,
        List<AiQuestionDraftResponse> questions,
        List<String> errors
) {
}
