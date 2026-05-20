package com.htto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MatchingPairRequest(
        @NotBlank String matchingId,
        @NotBlank String leftText,
        @NotBlank String rightText,
        Integer orderIndex
) {
}
