package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.Question;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record QuestionStudentResponse(
        String questionId,
        QuestionType type,
        String content,
        BigDecimal score,
        Difficulty difficulty,
        String topic,
        List<OptionForStudentResponse> options,
        FillBlankRuleResponse fillBlankRule,
        MatchingForStudentResponse matching
) {

    public static QuestionStudentResponse from(Question question) {
        var answer = question.getAnswerDefinition();
        return new QuestionStudentResponse(
                question.getId(),
                question.getType(),
                question.getContent(),
                question.getScore(),
                question.getDifficulty(),
                question.getTopic(),
                answer == null ? List.of() : answer.getOptions().stream().map(OptionForStudentResponse::from).toList(),
                answer == null ? null : FillBlankRuleResponse.from(answer.getFillBlankAnswer()),
                answer == null ? null : MatchingForStudentResponse.from(answer.getMatchingPairs())
        );
    }

    public record OptionForStudentResponse(String optionId, String content, int orderIndex) {

        public static OptionForStudentResponse from(com.htto.backend.domain.embedded.AnswerOption option) {
            return new OptionForStudentResponse(option.getOptionId(), option.getContent(), option.getOrderIndex());
        }
    }

    public record FillBlankRuleResponse(boolean ignoreCase, boolean ignoreAccent, boolean trimSpace) {

        public static FillBlankRuleResponse from(com.htto.backend.domain.embedded.FillBlankAnswer answer) {
            if (answer == null) {
                return null;
            }
            return new FillBlankRuleResponse(answer.isIgnoreCase(), answer.isIgnoreAccent(), answer.isTrimSpace());
        }
    }

    public record MatchingForStudentResponse(
            List<MatchingLeftItemResponse> leftItems,
            List<MatchingRightItemResponse> rightItems
    ) {

        public static MatchingForStudentResponse from(List<com.htto.backend.domain.embedded.MatchingPair> pairs) {
            if (pairs == null) {
                return null;
            }
            return new MatchingForStudentResponse(
                    pairs.stream()
                            .map(pair -> new MatchingLeftItemResponse(
                                    pair.getMatchingId(),
                                    pair.getLeftText(),
                                    pair.getOrderIndex()
                            ))
                            .toList(),
                    pairs.stream()
                            .map(pair -> new MatchingRightItemResponse(pair.getRightText()))
                            .sorted(Comparator.comparing(MatchingRightItemResponse::rightText))
                            .toList()
            );
        }
    }

    public record MatchingLeftItemResponse(String matchingId, String leftText, int orderIndex) {
    }

    public record MatchingRightItemResponse(String rightText) {
    }
}
