package com.moodandmove.analysis.dto;

public record BestActionResponse(
        Long actionId,
        String actionName,
        String category,
        Integer delta
) {
}
