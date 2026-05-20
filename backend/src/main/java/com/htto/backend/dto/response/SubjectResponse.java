package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.SubjectStatus;
import com.htto.backend.domain.Subject;
import java.time.Instant;

public record SubjectResponse(
        String subjectId,
        String subjectCode,
        String subjectName,
        String description,
        SubjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getDescription(),
                subject.getStatus(),
                subject.getCreatedAt(),
                subject.getUpdatedAt()
        );
    }
}
