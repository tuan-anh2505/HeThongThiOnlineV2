package com.htto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnswerOptionRequest(
        @NotBlank String optionId,
        @NotBlank String content,
        Boolean isCorrect,
        Integer orderIndex
) {
}
