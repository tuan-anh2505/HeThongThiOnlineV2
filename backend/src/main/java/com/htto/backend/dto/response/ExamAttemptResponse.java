package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ExamAttemptResponse(
        String attemptId,
        String studentId,
        String examId,
        String examName,
        String sessionId,
        Instant startedAt,
        Instant submittedAt,
        Instant deadline,
        long remainingSeconds,
        BigDecimal totalScore,
        ExamAttemptStatus status,
        int attemptNumber,
        int maxAttempts,
        int durationMinutes,
        List<ExamAttemptQuestionResponse> questions
) {

    public static ExamAttemptResponse from(ExamAttempt attempt, Exam exam, Instant now) {
        long remainingSeconds = attempt.getDeadline() == null
                ? 0
                : Math.max(0, Duration.between(now, attempt.getDeadline()).getSeconds());
        return new ExamAttemptResponse(
                attempt.getId(),
                attempt.getStudentId(),
                attempt.getExamId(),
                exam == null ? null : exam.getTitle(),
                attempt.getSessionId(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getDeadline(),
                remainingSeconds,
                attempt.getTotalScore(),
                attempt.getStatus(),
                attempt.getAttemptNumber(),
                exam == null ? 0 : exam.getMaxAttempts(),
                exam == null ? 0 : exam.getDurationMinutes(),
                attempt.getQuestionSnapshots()
                        .stream()
                        .sorted(Comparator.comparingInt(snapshot -> snapshot.getOrderIndex()))
                        .map(ExamAttemptQuestionResponse::from)
                        .toList()
        );
    }
}
