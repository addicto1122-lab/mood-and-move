package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPreferenceDto {

    private EnvironmentType environmentType;
    private ActivityStyle activityStyle;
    private SocialType socialType;
}
