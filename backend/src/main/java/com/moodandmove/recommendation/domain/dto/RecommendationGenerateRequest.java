package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.place.domain.type.LocationMode;

import java.math.BigDecimal;

public record RecommendationGenerateRequest(
        LocationMode locationMode,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
