package com.htto.backend.dto.request;

import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AccountUpdateRequest(
        String username,
        @Size(min = 8) String password,
        String fullName,
        LocalDate dateOfBirth,
        @Email String email,
        String phone,
        Role role,
        AccountStatus status
) {
}
