package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.embedded.ExamAttemptFillBlankRuleSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptMatchingSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptOptionSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record ExamAttemptQuestionResponse(
        String questionId,
        QuestionType type,
        String content,
        BigDecimal score,
        Difficulty difficulty,
        String topic,
        int orderIndex,
        List<OptionResponse> options,
        FillBlankRuleResponse fillBlankRule,
        MatchingResponse matching
) {

    public static ExamAttemptQuestionResponse from(ExamAttemptQuestionSnapshot snapshot) {
        return new ExamAttemptQuestionResponse(
                snapshot.getQuestionId(),
                snapshot.getType(),
                snapshot.getContent(),
                snapshot.getScore(),
                snapshot.getDifficulty(),
                snapshot.getTopic(),
                snapshot.getOrderIndex(),
                snapshot.getOptions() == null
                        ? List.of()
                        : snapshot.getOptions()
                                .stream()
                                .sorted(Comparator.comparingInt(ExamAttemptOptionSnapshot::getOrderIndex))
                                .map(OptionResponse::from)
                                .toList(),
                FillBlankRuleResponse.from(snapshot.getFillBlankRule()),
                MatchingResponse.from(snapshot.getMatching())
        );
    }

    public record OptionResponse(String optionId, String content, int orderIndex) {

        public static OptionResponse from(ExamAttemptOptionSnapshot snapshot) {
            return new OptionResponse(snapshot.getOptionId(), snapshot.getContent(), snapshot.getOrderIndex());
        }
    }

    public record FillBlankRuleResponse(boolean ignoreCase, boolean ignoreAccent, boolean trimSpace) {

        public static FillBlankRuleResponse from(ExamAttemptFillBlankRuleSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            return new FillBlankRuleResponse(
                    snapshot.isIgnoreCase(),
                    snapshot.isIgnoreAccent(),
                    snapshot.isTrimSpace()
            );
        }
    }

    public record MatchingResponse(
            List<MatchingLeftResponse> leftItems,
            List<MatchingRightResponse> rightItems
    ) {

        public static MatchingResponse from(ExamAttemptMatchingSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            return new MatchingResponse(
                    snapshot.getLeftItems()
                            .stream()
                            .map(item -> new MatchingLeftResponse(
                                    item.getMatchingId(),
                                    item.getLeftText(),
                                    item.getOrderIndex()
                            ))
                            .toList(),
                    snapshot.getRightItems()
                            .stream()
                            .map(item -> new MatchingRightResponse(item.getRightText(), item.getOrderIndex()))
                            .toList()
            );
        }
    }

    public record MatchingLeftResponse(String matchingId, String leftText, int orderIndex) {
    }

    public record MatchingRightResponse(String rightText, int orderIndex) {
    }
}
