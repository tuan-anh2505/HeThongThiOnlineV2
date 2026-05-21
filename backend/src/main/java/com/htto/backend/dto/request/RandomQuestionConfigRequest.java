package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionType;
import jakarta.validation.constraints.Min;

public record RandomQuestionConfigRequest(
        QuestionType type,
        Difficulty difficulty,
        String topic,
        @Min(1) Integer quantity
) {
}
