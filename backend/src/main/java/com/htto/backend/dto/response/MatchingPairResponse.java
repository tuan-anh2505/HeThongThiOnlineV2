package com.htto.backend.dto.response;

import com.htto.backend.domain.embedded.MatchingPair;

public record MatchingPairResponse(
        String matchingId,
        String leftText,
        String rightText,
        int orderIndex
) {

    public static MatchingPairResponse from(MatchingPair pair) {
        return new MatchingPairResponse(
                pair.getMatchingId(),
                pair.getLeftText(),
                pair.getRightText(),
                pair.getOrderIndex()
        );
    }
}
