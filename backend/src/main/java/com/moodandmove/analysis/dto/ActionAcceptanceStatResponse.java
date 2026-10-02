package com.moodandmove.analysis.dto;


public record ActionAcceptanceStatResponse(
        Long actionId,
        long recommendationCount,
        long executionCount
) {
}