package com.moodandmove.recommendation.domain.dto;

import java.math.BigDecimal;

public record CategoryScoreDto(
        String category,
        BigDecimal personalScore,
        Integer sampleCount
) {
}