package com.moodandmove.recommendation.domain.dto;

import java.util.List;

public record PreparedRecommendation(
        String requestId,
        LlmRecommendationRequestDto request,
        List<LlmRecommendedActionDto> actions
) {
    public PreparedRecommendation {
        actions = List.copyOf(actions);
    }
}