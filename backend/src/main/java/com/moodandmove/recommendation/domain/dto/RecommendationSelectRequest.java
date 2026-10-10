package com.moodandmove.recommendation.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecommendationSelectRequest(
        @NotNull(message = "선택할 추천 ID는 필수입니다.")
        @Positive(message = "추천 ID는 양수여야 합니다.")
        Long recommendationId
) {
}