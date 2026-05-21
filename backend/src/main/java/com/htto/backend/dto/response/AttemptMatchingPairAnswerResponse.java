package com.htto.backend.dto.response;

import com.htto.backend.domain.embedded.AttemptMatchingPairAnswer;

public record AttemptMatchingPairAnswerResponse(
        String leftId,
        String rightText
) {

    public static AttemptMatchingPairAnswerResponse from(AttemptMatchingPairAnswer answer) {
        if (answer == null) {
            return null;
        }
        return new AttemptMatchingPairAnswerResponse(answer.getLeftId(), answer.getRightText());
    }
}
