package com.moodandmove.recommendation.domain.dto;

import java.math.BigDecimal;

public record ActionCandidateDto(
        Long actionId,
        String actionName,
        double score,
        long sampleCount
) {
    // sampleCount 없이 생성할 때
    public ActionCandidateDto(
            Long actionId,
            String actionName,
            BigDecimal score
    ) {
        this(actionId, actionName, score.doubleValue(), 0L);
    }

    // BigDecimal 점수와 sampleCount를 함께 받을 때
    public ActionCandidateDto(
            Long actionId,
            String actionName,
            BigDecimal score,
            long sampleCount
    ) {
        this(actionId, actionName, score.doubleValue(), sampleCount);
    }
}