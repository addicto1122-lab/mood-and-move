package com.moodandmove.analysis.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyStatsResponse(
        int year,
        int month,

        Long diaryCount,
        BigDecimal averageMoodScore,

        Long recommendationCount,
        Long executionCount,
        Long sampleCount,
        Long positiveCount,

        BigDecimal executionRate,
        BigDecimal positiveRate,
        BigDecimal averageDelta,

        List<EmotionStatResponse> emotions
) {
}
