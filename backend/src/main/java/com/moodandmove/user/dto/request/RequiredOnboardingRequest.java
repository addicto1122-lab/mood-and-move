package com.moodandmove.user.dto.request;

import com.moodandmove.user.domain.type.AgeGroup;
import com.moodandmove.user.domain.type.Gender;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RequiredOnboardingRequest(

        @NotNull(message = "나이대를 선택해주세요.")
        AgeGroup ageGroup,

        @NotNull(message = "성별을 선택해주세요.")
        Gender gender,

        @NotEmpty(message = "좋아하는 활동을 하나 이상 선택해주세요.")
        List<@NotNull Long> hobbyIds

) {
}