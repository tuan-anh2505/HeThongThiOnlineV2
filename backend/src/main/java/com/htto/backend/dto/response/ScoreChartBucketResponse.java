package com.htto.backend.dto.response;

public record ScoreChartBucketResponse(
        String label,
        int fromPercent,
        int toPercent,
        int count
) {
}
