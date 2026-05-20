package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.ClassStatus;
import jakarta.validation.constraints.NotBlank;

public record ClassCreateRequest(
        @NotBlank String classCode,
        @NotBlank String className,
        String teacherId,
        ClassStatus status
) {
}
