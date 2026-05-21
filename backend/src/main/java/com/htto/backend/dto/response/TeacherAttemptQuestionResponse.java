package com.htto.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.htto.backend.domain.DomainEnums.AttemptAnswerStatus;
import java.math.BigDecimal;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherAttemptQuestionResponse(
        ExamAttemptQuestionResponse question,
        AttemptAnswerValueResponse studentAnswer,
        AttemptAnswerStatus answerStatus,
        Boolean isCorrect,
        BigDecimal scoreAchieved,
        Map<String, Object> correctAnswer
) {
}
