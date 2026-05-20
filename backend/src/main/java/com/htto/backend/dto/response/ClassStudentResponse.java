package com.htto.backend.dto.response;

import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import java.time.Instant;

public record ClassStudentResponse(
        String id,
        String classId,
        String studentId,
        String studentCode,
        Instant joinedAt,
        EnrollmentStatus status,
        AccountResponse account
) {

    public static ClassStudentResponse from(
            ClassStudent classStudent,
            String studentCode,
            AccountResponse account
    ) {
        return new ClassStudentResponse(
                classStudent.getId(),
                classStudent.getClassId(),
                classStudent.getStudentId(),
                studentCode,
                classStudent.getJoinedAt(),
                classStudent.getStatus(),
                account
        );
    }
}
