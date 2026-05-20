package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.AcademicStatus;
import com.htto.backend.domain.StudentProfile;

public record StudentProfileResponse(
        String studentId,
        String accountId,
        String studentCode,
        String mainClassId,
        AcademicStatus status,
        AccountResponse account
) {

    public static StudentProfileResponse from(StudentProfile profile, AccountResponse account) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getAccountId(),
                profile.getStudentCode(),
                profile.getMainClassId(),
                profile.getStatus(),
                account
        );
    }
}
