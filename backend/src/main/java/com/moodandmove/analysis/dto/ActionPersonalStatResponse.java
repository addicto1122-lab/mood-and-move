package com.moodandmove.analysis.dto;

import com.moodandmove.analysis.domain.type.ConfidenceLevel;

import java.math.BigDecimal;


public record ActionPersonalStatResponse(
        Long actionId,

        String emotionCode,

        long recommendationCount,

        long executionCount,

        long sampleCount,

        long positiveCount,

        BigDecimal avgDelta,

        ConfidenceLevel confidenceLevel
) {
}
