package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import com.htto.backend.domain.Exam;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ExamResponse(
        String examId,
        String examName,
        List<String> classIds,
        String subjectId,
        String questionBankId,
        String teacherId,
        int durationMinutes,
        int maxAttempts,
        boolean allowViewScore,
        boolean allowReview,
        boolean allowViewCorrectAnswer,
        boolean shuffleQuestions,
        boolean shuffleOptions,
        BigDecimal totalScore,
        ResultPublishStatus resultStatus,
        ExamStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<ExamQuestionResponse> questions,
        List<ClassResponse> classes,
        SubjectResponse subject,
        QuestionBankResponse questionBank,
        TeacherProfileResponse teacher
) {

    public static ExamResponse from(
            Exam exam,
            List<ExamQuestionResponse> questions,
            List<ClassResponse> classes,
            SubjectResponse subject,
            QuestionBankResponse questionBank,
            TeacherProfileResponse teacher
    ) {
        return new ExamResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getClassIds(),
                exam.getSubjectId(),
                exam.getQuestionBankId(),
                exam.getTeacherId(),
                exam.getDurationMinutes(),
                exam.getMaxAttempts(),
                exam.getSettings() != null && exam.getSettings().isShowScoreImmediately(),
                exam.getSettings() != null && exam.getSettings().isAllowReview(),
                exam.getSettings() != null && exam.getSettings().isShowCorrectAnswers(),
                exam.getSettings() != null && exam.getSettings().isShuffleQuestions(),
                exam.getSettings() != null && exam.getSettings().isShuffleAnswers(),
                exam.getTotalScore(),
                exam.getResultStatus(),
                exam.getStatus(),
                exam.getCreatedAt(),
                exam.getUpdatedAt(),
                questions,
                classes,
                subject,
                questionBank,
                teacher
        );
    }
}
