package com.htto.backend.dto.request;

import java.util.List;

public record AttemptAnswerValueRequest(
        Boolean trueFalseAnswer,
        String selectedOptionId,
        String fillBlankText,
        List<AttemptMatchingPairAnswerRequest> matchingPairs
) {
}
