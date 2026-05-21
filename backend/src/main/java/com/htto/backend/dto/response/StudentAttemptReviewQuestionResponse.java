package com.htto.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.htto.backend.domain.DomainEnums.AttemptAnswerStatus;
import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionType;
import java.math.BigDecimal;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentAttemptReviewQuestionResponse(
        String questionId,
        QuestionType type,
        String content,
        BigDecimal score,
        Difficulty difficulty,
        String topic,
        int orderIndex,
        ExamAttemptQuestionResponse question,
        AttemptAnswerValueResponse studentAnswer,
        AttemptAnswerStatus answerStatus,
        Boolean isCorrect,
        BigDecimal scoreAchieved,
        Map<String, Object> correctAnswer
) {
}
