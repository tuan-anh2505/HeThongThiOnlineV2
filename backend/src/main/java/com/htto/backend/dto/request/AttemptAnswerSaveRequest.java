package com.htto.backend.dto.request;

import java.util.List;

public record AttemptAnswerSaveRequest(
        String questionId,
        AttemptAnswerValueRequest studentAnswer,
        Boolean trueFalseAnswer,
        String selectedOptionId,
        String fillBlankText,
        List<AttemptMatchingPairAnswerRequest> matchingPairs
) {
}
