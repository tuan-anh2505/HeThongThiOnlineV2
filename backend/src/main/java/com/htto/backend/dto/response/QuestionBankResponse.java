package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import com.htto.backend.domain.QuestionBank;
import java.time.Instant;

public record QuestionBankResponse(
        String questionBankId,
        String name,
        String description,
        String subjectId,
        String teacherId,
        QuestionBankStatus status,
        Instant createdAt,
        Instant updatedAt,
        SubjectResponse subject,
        TeacherProfileResponse teacher
) {

    public static QuestionBankResponse from(
            QuestionBank questionBank,
            SubjectResponse subject,
            TeacherProfileResponse teacher
    ) {
        return new QuestionBankResponse(
                questionBank.getId(),
                questionBank.getName(),
                questionBank.getDescription(),
                questionBank.getSubjectId(),
                questionBank.getTeacherId(),
                questionBank.getStatus(),
                questionBank.getCreatedAt(),
                questionBank.getUpdatedAt(),
                subject,
                teacher
        );
    }
}
