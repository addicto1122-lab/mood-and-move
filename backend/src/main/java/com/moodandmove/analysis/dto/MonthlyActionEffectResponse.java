package com.moodandmove.analysis.dto;

import java.util.List;

public record MonthlyActionEffectResponse(
        BestActionResponse bestAction,

        List<CategoryExecutionRankResponse> executionRanking,

        List<CategoryEffectRankResponse> effectRanking
) {
}
