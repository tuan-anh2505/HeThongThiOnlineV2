package com.htto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @NotBlank String newPassword,
        @NotBlank String confirmPassword
) {
}
