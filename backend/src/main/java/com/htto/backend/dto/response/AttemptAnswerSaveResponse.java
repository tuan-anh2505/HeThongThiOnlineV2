package com.htto.backend.dto.response;

import java.time.Instant;

public record AttemptAnswerSaveResponse(
        boolean saved,
        Instant updatedAt
) {
}
