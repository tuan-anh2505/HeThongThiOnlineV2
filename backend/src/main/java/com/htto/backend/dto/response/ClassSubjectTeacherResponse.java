package com.htto.backend.dto.response;

import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import java.time.Instant;

public record ClassSubjectTeacherResponse(
        String id,
        String classId,
        String subjectId,
        String teacherId,
        String semester,
        String schoolYear,
        AssignmentStatus status,
        Instant createdAt,
        Instant updatedAt,
        ClassResponse schoolClass,
        SubjectResponse subject,
        TeacherProfileResponse teacher
) {

    public static ClassSubjectTeacherResponse from(
            ClassSubjectTeacher assignment,
            ClassResponse schoolClass,
            SubjectResponse subject,
            TeacherProfileResponse teacher
    ) {
        return new ClassSubjectTeacherResponse(
                assignment.getId(),
                assignment.getClassId(),
                assignment.getSubjectId(),
                assignment.getTeacherId(),
                assignment.getSemester(),
                assignment.getSchoolYear(),
                assignment.getStatus(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt(),
                schoolClass,
                subject,
                teacher
        );
    }
}
