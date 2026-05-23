package com.htto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AttemptMatchingPairAnswerRequest(
        String leftId,
        String matchingId,
        @NotBlank String rightText
) {
}
