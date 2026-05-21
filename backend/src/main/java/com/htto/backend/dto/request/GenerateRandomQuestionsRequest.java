package com.htto.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GenerateRandomQuestionsRequest(
        @NotEmpty List<@Valid RandomQuestionConfigRequest> configs
) {
}
