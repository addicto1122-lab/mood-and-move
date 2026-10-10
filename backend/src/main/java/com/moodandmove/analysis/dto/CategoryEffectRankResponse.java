package com.moodandmove.analysis.dto;

import java.math.BigDecimal;

public record CategoryEffectRankResponse(
        String category,
        BigDecimal averageDelta,
        Long sampleCount
) {
}
