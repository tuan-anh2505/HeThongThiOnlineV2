package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.SubjectStatus;
import jakarta.validation.constraints.NotBlank;

public record SubjectCreateRequest(
        @NotBlank String subjectCode,
        @NotBlank String subjectName,
        String description,
        SubjectStatus status
) {
}
