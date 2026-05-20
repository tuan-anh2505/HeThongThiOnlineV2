package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record QuestionCreateRequest(
        @NotNull QuestionType type,
        @NotBlank String content,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal score,
        @NotNull Difficulty difficulty,
        String topic,
        QuestionStatus status,
        @NotNull @Valid AnswerDefinitionRequest answer
) {
}
