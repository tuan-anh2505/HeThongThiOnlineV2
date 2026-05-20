package com.htto.backend.dto.request;

import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AccountCreateRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String fullName,
        LocalDate dateOfBirth,
        @NotBlank @Email String email,
        String phone,
        @NotNull Role role,
        AccountStatus status
) {
}
