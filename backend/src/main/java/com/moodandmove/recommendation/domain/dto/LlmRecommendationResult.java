package com.moodandmove.recommendation.domain.dto;

import java.util.List;

public record LlmRecommendationResult(
        List<LlmRecommendedActionDto> recommendations
) {
}
