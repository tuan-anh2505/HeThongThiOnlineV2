package com.htto.backend.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record AttemptAnswerValueRequest(
        Boolean trueFalseAnswer,
        String selectedOptionId,
        String fillBlankText,
        List<@Valid AttemptMatchingPairAnswerRequest> matchingPairs
) {
}
