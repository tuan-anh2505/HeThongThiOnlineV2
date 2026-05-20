package com.htto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddStudentToClassRequest(
        @NotBlank String studentId
) {
}
