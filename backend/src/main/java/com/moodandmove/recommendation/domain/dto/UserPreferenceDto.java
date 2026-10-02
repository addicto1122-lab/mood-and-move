package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;


public record UserPreferenceDto(
        EnvironmentType environmentType,
        ActivityStyle activityStyle,
        SocialType socialType
) {


}
