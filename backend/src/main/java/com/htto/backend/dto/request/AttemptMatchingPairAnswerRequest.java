package com.htto.backend.dto.request;

public record AttemptMatchingPairAnswerRequest(
        String leftId,
        String matchingId,
        String rightText
) {
}
