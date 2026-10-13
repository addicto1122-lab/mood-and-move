package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;

import java.util.List;


public record UserPreferenceDto(
        EnvironmentType environmentType,
        ActivityStyle activityStyle,
        SocialType socialType,
        Integer defaultAvailableMinutes,
        List<String> hobbies
) {
}
