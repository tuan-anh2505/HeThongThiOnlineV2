package com.htto.backend.dto.request;

import com.htto.backend.domain.DomainEnums.ImportSourceType;
import jakarta.validation.constraints.NotBlank;

public record ImportQuestionsFromUrlRequest(
        @NotBlank String url,
        ImportSourceType sourceType
) {
}
