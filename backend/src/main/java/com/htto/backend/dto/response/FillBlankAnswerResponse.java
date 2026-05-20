package com.htto.backend.dto.response;

import com.htto.backend.domain.embedded.FillBlankAnswer;
import java.util.List;

public record FillBlankAnswerResponse(
        String answerId,
        List<String> acceptedAnswers,
        boolean ignoreCase,
        boolean ignoreAccent,
        boolean trimSpace
) {

    public static FillBlankAnswerResponse from(FillBlankAnswer answer) {
        if (answer == null) {
            return null;
        }
        return new FillBlankAnswerResponse(
                answer.getAnswerId(),
                answer.getAcceptedAnswers(),
                answer.isIgnoreCase(),
                answer.isIgnoreAccent(),
                answer.isTrimSpace()
        );
    }
}
