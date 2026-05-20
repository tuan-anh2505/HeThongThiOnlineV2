package com.htto.backend.dto.response;

public record QuestionImportErrorResponse(
        int questionIndex,
        int lineNumber,
        String message
) {
}
