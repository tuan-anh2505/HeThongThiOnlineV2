package com.htto.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ExamSessionCreateRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {
}
