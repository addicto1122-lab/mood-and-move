package com.moodandmove.mood.dto.response;

import com.moodandmove.recommendation.domain.dto.response.RecommendationResponse;

public record MoodCreateWithRecommendationResponse(
        Long moodEntryId,
        RecommendationResponse recommendation
) {
}