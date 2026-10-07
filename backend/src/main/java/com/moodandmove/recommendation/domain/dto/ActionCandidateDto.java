package com.moodandmove.recommendation.domain.dto;

import java.math.BigDecimal;

public record ActionCandidateDto(
        String actionCode,
        String actionName,
        BigDecimal score,
        long sampleCount
) {
    public ActionCandidateDto(
            String actionCode,
            String actionName,
            BigDecimal score
    ) {
        this(actionCode, actionName, score, 0L);
    }
}