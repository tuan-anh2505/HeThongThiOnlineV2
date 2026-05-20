package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import jakarta.validation.constraints.NotBlank;

public record QuestionBankCreateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String subjectId,
        String teacherId,
        QuestionBankStatus status
) {
}
