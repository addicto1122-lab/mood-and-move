
package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;

public record LlmRecommendedActionDto(
        String actionName,
        String category,
        Integer durationMinutes,
        EnvironmentType environmentType,
        SocialType socialType,
        ActivityStyle activityStyle,
        boolean locationRequired,
        String placeCategory,
        String reason
) {
}
