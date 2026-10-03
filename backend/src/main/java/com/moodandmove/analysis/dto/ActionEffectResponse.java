package com.moodandmove.analysis.dto;

import java.math.BigDecimal;

public record ActionEffectResponse (
        Long actionId,
        String actionName,
        Long recommendationCount,
        Long executionCount,
        Long sampleCount,
        Long positiveCount,
        BigDecimal positiveRate,
        BigDecimal averageDelta
){
}
