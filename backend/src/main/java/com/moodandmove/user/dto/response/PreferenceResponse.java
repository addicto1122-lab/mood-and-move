package com.moodandmove.user.dto.response;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;

import java.math.BigDecimal;
import java.util.List;

public record PreferenceResponse(

        List<Long> hobbyIds,

        ActivityStyle activityStyle,

        EnvironmentType activityEnvironment,

        SocialType socialPreference,

        Integer defaultAvailableMinutes,

        String defaultRegionName,

        String defaultRegionCode,

        BigDecimal defaultRegionLatitude,

        BigDecimal defaultRegionLongitude

) {
}