package com.htto.backend.dto.response;

import com.htto.backend.domain.Role;

public record ProfileMeResponse(
        Role role,
        AccountResponse account,
        StudentProfileResponse studentProfile,
        TeacherProfileResponse teacherProfile,
        AdminProfileResponse adminProfile
) {
}
