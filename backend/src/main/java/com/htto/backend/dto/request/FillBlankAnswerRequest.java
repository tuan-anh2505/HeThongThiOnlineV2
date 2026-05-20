package com.htto.backend.dto.request;

import java.util.List;

public record FillBlankAnswerRequest(
        String answerId,
        List<String> acceptedAnswers,
        Boolean ignoreCase,
        Boolean ignoreAccent,
        Boolean trimSpace
) {
}
