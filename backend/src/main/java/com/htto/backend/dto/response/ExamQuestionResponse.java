package com.htto.backend.dto.response;

import com.htto.backend.domain.ExamQuestion;
import com.htto.backend.domain.embedded.ExamQuestionRef;
import java.math.BigDecimal;
import java.time.Instant;

public record ExamQuestionResponse(
        String id,
        String examId,
        String questionId,
        BigDecimal score,
        int orderIndex,
        Instant createdAt,
        Instant updatedAt,
        QuestionResponse question
) {

    public static ExamQuestionResponse from(ExamQuestion examQuestion, QuestionResponse question) {
        return new ExamQuestionResponse(
                examQuestion.getId(),
                examQuestion.getExamId(),
                examQuestion.getQuestionId(),
                examQuestion.getScore(),
                examQuestion.getOrderIndex(),
                examQuestion.getCreatedAt(),
                examQuestion.getUpdatedAt(),
                question
        );
    }

    public static ExamQuestionResponse fromRef(String examId, ExamQuestionRef questionRef, QuestionResponse question) {
        return new ExamQuestionResponse(
                null,
                examId,
                questionRef.getQuestionId(),
                questionRef.getPoint(),
                questionRef.getDisplayOrder(),
                null,
                null,
                question
        );
    }
}
