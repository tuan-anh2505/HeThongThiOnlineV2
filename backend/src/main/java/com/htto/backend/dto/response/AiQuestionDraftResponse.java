package com.htto.backend.dto.response;

import com.htto.backend.dto.request.QuestionCreateRequest;

public record AiQuestionDraftResponse(
        int questionIndex,
        boolean valid,
        String error,
        QuestionCreateRequest question
) {
}
