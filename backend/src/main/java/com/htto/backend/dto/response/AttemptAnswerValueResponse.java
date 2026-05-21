package com.htto.backend.dto.response;

import com.htto.backend.domain.embedded.AttemptAnswerValue;
import java.util.List;

public record AttemptAnswerValueResponse(
        Boolean trueFalseAnswer,
        String selectedOptionId,
        String fillBlankText,
        List<AttemptMatchingPairAnswerResponse> matchingPairs
) {

    public static AttemptAnswerValueResponse from(AttemptAnswerValue answer) {
        if (answer == null) {
            return null;
        }
        return new AttemptAnswerValueResponse(
                answer.getTrueFalseAnswer(),
                answer.getSelectedOptionId(),
                answer.getFillBlankText(),
                answer.getMatchingPairs() == null
                        ? List.of()
                        : answer.getMatchingPairs()
                                .stream()
                                .map(AttemptMatchingPairAnswerResponse::from)
                                .toList()
        );
    }
}
