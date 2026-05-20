package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ProfileStatus;
import com.htto.backend.domain.TeacherProfile;

public record TeacherProfileResponse(
        String teacherId,
        String accountId,
        String teacherCode,
        ProfileStatus status,
        AccountResponse account
) {

    public static TeacherProfileResponse from(TeacherProfile profile, AccountResponse account) {
        return new TeacherProfileResponse(
                profile.getId(),
                profile.getAccountId(),
                profile.getTeacherCode(),
                profile.getStatus(),
                account
        );
    }
}
