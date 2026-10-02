package com.moodandmove.recommendation.domain.dto.response;

public record DislikeActionResponse(

        Long actionId,

        String name,

        String category,

        Integer durationMinutes,

        boolean disliked

) {
}