package com.htto.backend.dto.response;

import com.htto.backend.domain.embedded.AnswerOption;

public record AnswerOptionResponse(
        String optionId,
        String content,
        boolean isCorrect,
        int orderIndex
) {

    public static AnswerOptionResponse from(AnswerOption option) {
        return new AnswerOptionResponse(
                option.getOptionId(),
                option.getContent(),
                option.isCorrect(),
                option.getOrderIndex()
        );
    }
}
