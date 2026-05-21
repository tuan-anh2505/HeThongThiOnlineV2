package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import java.time.Instant;
import java.util.List;

public record StudentExamDetailResponse(
        String examId,
        String examName,
        String subjectId,
        String subjectName,
        String classId,
        String className,
        int durationMinutes,
        int maxAttempts,
        boolean hasPassword,
        boolean allowViewScore,
        boolean allowReview,
        boolean allowViewCorrectAnswer,
        boolean shuffleQuestions,
        boolean shuffleOptions,
        Instant startTime,
        Instant endTime,
        ExamStatus status,
        ExamSessionStatus sessionStatus,
        String attemptStatus,
        int attemptCount,
        List<QuestionStudentResponse> questions
) {
}
