package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.ProfileStatus;
import jakarta.validation.constraints.NotBlank;

public record TeacherProfileCreateRequest(
        @NotBlank String teacherCode,
        ProfileStatus status
) {
}
