package com.htto.backend.dto.response;

import com.htto.backend.domain.embedded.AnswerDefinition;
import java.util.List;

public record AnswerDefinitionResponse(
        Boolean correctAnswer,
        List<AnswerOptionResponse> options,
        FillBlankAnswerResponse fillBlank,
        List<MatchingPairResponse> matchingPairs
) {

    public static AnswerDefinitionResponse from(AnswerDefinition answerDefinition) {
        if (answerDefinition == null) {
            return null;
        }
        return new AnswerDefinitionResponse(
                answerDefinition.getTrueFalseAnswer(),
                answerDefinition.getOptions().stream().map(AnswerOptionResponse::from).toList(),
                FillBlankAnswerResponse.from(answerDefinition.getFillBlankAnswer()),
                answerDefinition.getMatchingPairs().stream().map(MatchingPairResponse::from).toList()
        );
    }
}
