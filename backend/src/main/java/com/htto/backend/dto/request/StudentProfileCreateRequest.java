package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.AcademicStatus;
import jakarta.validation.constraints.NotBlank;

public record StudentProfileCreateRequest(
        @NotBlank String studentCode,
        String mainClassId,
        AcademicStatus status
) {
}
