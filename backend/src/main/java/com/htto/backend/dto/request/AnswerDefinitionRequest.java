package com.htto.backend.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record AnswerDefinitionRequest(
        Boolean correctAnswer,
        List<@Valid AnswerOptionRequest> options,
        @Valid FillBlankAnswerRequest fillBlank,
        List<@Valid MatchingPairRequest> matchingPairs
) {
}
