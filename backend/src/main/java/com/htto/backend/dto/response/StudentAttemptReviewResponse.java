package com.htto.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentAttemptReviewResponse(
        String attemptId,
        String examId,
        String examName,
        String subjectName,
        int attemptNumber,
        ExamAttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        boolean scoreVisible,
        BigDecimal totalScore,
        BigDecimal maxScore,
        boolean correctAnswerVisible,
        List<StudentAttemptReviewQuestionResponse> questions
) {
}
