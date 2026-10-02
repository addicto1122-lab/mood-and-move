package com.moodandmove.analysis.dto;

import java.math.BigDecimal;

public record ActionPersonalStatResponse(
        Long actionId,

        String emotionCode,

        long recommendationCount,

        long executionCount,

        long sampleCount,

        long positiveCount,

        BigDecimal avgDelta
) {
}
