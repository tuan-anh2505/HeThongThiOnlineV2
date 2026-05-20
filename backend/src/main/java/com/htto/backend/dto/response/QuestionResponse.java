package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.Question;
import java.math.BigDecimal;
import java.time.Instant;

public record QuestionResponse(
        String questionId,
        String questionBankId,
        QuestionType type,
        String content,
        BigDecimal score,
        Difficulty difficulty,
        String topic,
        QuestionStatus status,
        Instant createdAt,
        Instant updatedAt,
        AnswerDefinitionResponse answer
) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestionBankId(),
                question.getType(),
                question.getContent(),
                question.getScore(),
                question.getDifficulty(),
                question.getTopic(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                AnswerDefinitionResponse.from(question.getAnswerDefinition())
        );
    }
}
