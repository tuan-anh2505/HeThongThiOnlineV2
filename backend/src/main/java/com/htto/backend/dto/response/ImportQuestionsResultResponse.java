package com.htto.backend.dto.response;

import java.util.List;

public record ImportQuestionsResultResponse(
        boolean success,
        int importedCount,
        int failedCount,
        List<String> errors
) {

    public static ImportQuestionsResultResponse success(int importedCount) {
        return new ImportQuestionsResultResponse(true, importedCount, 0, List.of());
    }

    public static ImportQuestionsResultResponse failed(List<String> errors) {
        return new ImportQuestionsResultResponse(false, 0, errors.size(), errors);
    }
}
