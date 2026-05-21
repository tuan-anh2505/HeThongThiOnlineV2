package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.ExamSession;
import java.time.Instant;

public record ExamSessionResponse(
        String examSessionId,
        String examId,
        Instant startTime,
        Instant endTime,
        ExamSessionStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ExamSessionResponse from(ExamSession examSession) {
        return new ExamSessionResponse(
                examSession.getId(),
                examSession.getExamId(),
                examSession.getStartTime(),
                examSession.getEndTime(),
                examSession.getStatus(),
                examSession.getCreatedAt(),
                examSession.getUpdatedAt()
        );
    }
}
