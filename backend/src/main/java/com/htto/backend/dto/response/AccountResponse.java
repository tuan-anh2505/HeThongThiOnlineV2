package com.htto.backend.dto.response;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.Role;
import java.time.Instant;
import java.time.LocalDate;

public record AccountResponse(
        String id,
        String username,
        String fullName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        Role role,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getFullName(),
                account.getDateOfBirth(),
                account.getEmail(),
                account.getPhone(),
                account.getRole(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
