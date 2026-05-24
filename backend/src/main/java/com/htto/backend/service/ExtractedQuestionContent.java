package com.htto.backend.service;

import com.htto.backend.domain.DomainEnums.ImportSourceType;

public record ExtractedQuestionContent(
        ImportSourceType sourceType,
        String text
) {
}
