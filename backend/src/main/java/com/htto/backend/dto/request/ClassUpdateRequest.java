package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.ClassStatus;

public record ClassUpdateRequest(
        String classCode,
        String className,
        String teacherId,
        ClassStatus status
) {
}
