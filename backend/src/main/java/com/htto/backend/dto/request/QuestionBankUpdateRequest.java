package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.QuestionBankStatus;

public record QuestionBankUpdateRequest(
        String name,
        String description,
        String subjectId,
        String teacherId,
        QuestionBankStatus status
) {
}
