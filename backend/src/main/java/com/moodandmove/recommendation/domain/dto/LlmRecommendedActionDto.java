package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;

public record LlmRecommendedActionDto(
        String actionName,
        String category,
        Integer durationMinutes,
        Integer durationDays,
        Integer durationNights,
        EnvironmentType environmentType,
        SocialType socialType,
        ActivityStyle activityStyle,
        boolean locationRequired,
        String placeCategory,
        String reason,
        String emoji
) {

    public boolean hasValidDuration() {
        boolean minuteBased =
                durationMinutes != null
                        && durationMinutes > 0
                        && durationDays == null
                        && durationNights == null;

        boolean dayBased =
                durationMinutes == null
                        && durationDays != null
                        && durationDays >= 1
                        && durationNights != null
                        && durationNights >= 0
                        && durationNights < durationDays;

        return minuteBased || dayBased;
    }
}