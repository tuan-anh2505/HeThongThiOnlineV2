package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TeacherAttemptDetailResponse(
        String attemptId,
        String examId,
        String examName,
        String subjectName,
        String studentId,
        String studentCode,
        String studentName,
        String studentEmail,
        Instant startedAt,
        Instant submittedAt,
        ExamAttemptStatus status,
        BigDecimal totalScore,
        BigDecimal maxScore,
        int attemptNumber,
        List<TeacherAttemptQuestionResponse> questions
) {
}
