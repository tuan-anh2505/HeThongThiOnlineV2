package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import jakarta.validation.constraints.NotBlank;

public record ClassSubjectTeacherCreateRequest(
        @NotBlank String classId,
        @NotBlank String subjectId,
        @NotBlank String teacherId,
        String semester,
        String schoolYear,
        AssignmentStatus status
) {
}
