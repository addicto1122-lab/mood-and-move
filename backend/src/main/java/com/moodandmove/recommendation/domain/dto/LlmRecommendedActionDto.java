package com.moodandmove.recommendation.domain.dto;

public record LlmRecommendedActionDto(
        String actionCode,
        String reason
) {
}
