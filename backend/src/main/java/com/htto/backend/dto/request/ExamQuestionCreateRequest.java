package com.htto.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ExamQuestionCreateRequest(
        @NotBlank String questionId,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal score,
        Integer orderIndex
) {
}
