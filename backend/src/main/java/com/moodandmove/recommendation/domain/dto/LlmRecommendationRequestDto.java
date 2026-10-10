package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.recommendation.domain.type.RecommendationType;

import java.util.List;

public record LlmRecommendationRequestDto(
        CurrentStateDto currentState,
        RecommendationLocationDto location,
        UserPreferenceDto preference,
        List<CategoryScoreDto> categoryScores,
        RecommendationType recommendationType
) {
}