package com.moodandmove.mood.dto.request;

import com.moodandmove.recommendation.domain.dto.RecommendationGenerateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record MoodCreateWithRecommendationRequest(
        @NotNull
        @Valid
        MoodCreateRequest mood,

        @NotNull
        @Valid
        RecommendationGenerateRequest recommendation
) {
}