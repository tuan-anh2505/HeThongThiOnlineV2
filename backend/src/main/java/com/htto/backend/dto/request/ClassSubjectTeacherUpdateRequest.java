package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.AssignmentStatus;

public record ClassSubjectTeacherUpdateRequest(
        String classId,
        String subjectId,
        String teacherId,
        String semester,
        String schoolYear,
        AssignmentStatus status
) {
}
