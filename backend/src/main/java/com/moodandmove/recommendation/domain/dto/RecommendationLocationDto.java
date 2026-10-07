package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.place.domain.type.LocationMode;

import java.math.BigDecimal;

public record RecommendationLocationDto(
        LocationMode locationMode,
        String regionName,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
