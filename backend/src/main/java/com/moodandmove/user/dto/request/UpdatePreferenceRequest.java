package com.moodandmove.user.dto.request;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePreferenceRequest(

        @NotEmpty(message = "좋아하는 활동을 하나 이상 선택해주세요.")
        List<@NotNull Long> hobbyIds,

        @NotNull(message = "활동 성향을 선택해주세요.")
        ActivityStyle activityStyle,

        @NotNull(message = "활동 환경을 선택해주세요.")
        EnvironmentType activityEnvironment,

        @NotNull(message = "활동 방식을 선택해주세요.")
        SocialType socialPreference,

        @Min(value = 1, message = "활동 가능 시간은 1분 이상이어야 합니다.")
        Integer defaultAvailableMinutes,

        String defaultRegionName,

        String defaultRegionCode,

        BigDecimal defaultRegionLatitude,

        BigDecimal defaultRegionLongitude

) {
}