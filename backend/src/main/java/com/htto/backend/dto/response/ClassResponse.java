package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ClassStatus;
import com.htto.backend.domain.SchoolClass;
import java.time.Instant;

public record ClassResponse(
        String classId,
        String classCode,
        String className,
        int studentCount,
        String teacherId,
        ClassStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ClassResponse from(SchoolClass schoolClass) {
        return new ClassResponse(
                schoolClass.getId(),
                schoolClass.getClassCode(),
                schoolClass.getClassName(),
                schoolClass.getStudentCount(),
                schoolClass.getTeacherId(),
                schoolClass.getStatus(),
                schoolClass.getCreatedAt(),
                schoolClass.getUpdatedAt()
        );
    }
}
