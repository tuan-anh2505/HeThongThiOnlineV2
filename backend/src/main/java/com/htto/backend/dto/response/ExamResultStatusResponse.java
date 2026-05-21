package com.htto.backend.dto.response;

import com.htto.backend.domain.DomainEnums.ResultPublishStatus;

public record ExamResultStatusResponse(
        String examId,
        ResultPublishStatus resultStatus
) {
}
