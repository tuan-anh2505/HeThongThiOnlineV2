package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.SubjectStatus;

public record SubjectUpdateRequest(
        String subjectCode,
        String subjectName,
        String description,
        SubjectStatus status
) {
}
