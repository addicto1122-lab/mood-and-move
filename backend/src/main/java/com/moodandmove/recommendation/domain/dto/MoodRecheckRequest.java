package com.moodandmove.recommendation.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoodRecheckRequest(
        @NotNull(message = "재측정 기분 점수는 필수입니다.")
        @Min(value = 0, message = "기분 점수는 0 이상이어야 합니다.")
        @Max(value = 80, message = "기분 점수는 80 이하여야 합니다.")
        Integer afterScore
) {
}