package com.htto.backend.dto.request;

import java.time.Instant;

public record ExamSessionUpdateRequest(
        Instant startTime,
        Instant endTime
) {
}
