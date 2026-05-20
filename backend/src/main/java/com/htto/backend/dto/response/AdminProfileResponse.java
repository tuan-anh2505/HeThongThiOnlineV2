package com.htto.backend.dto.response;

import com.htto.backend.domain.AdminProfile;

public record AdminProfileResponse(
        String adminId,
        String accountId,
        String adminCode,
        AccountResponse account
) {

    public static AdminProfileResponse from(AdminProfile profile, AccountResponse account) {
        return new AdminProfileResponse(
                profile.getId(),
                profile.getAccountId(),
                profile.getAdminCode(),
                account
        );
    }
}
