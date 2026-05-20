package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record QuestionUpdateRequest(
        QuestionType type,
        String content,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal score,
        Difficulty difficulty,
        String topic,
        QuestionStatus status,
        @Valid AnswerDefinitionRequest answer
) {
}
