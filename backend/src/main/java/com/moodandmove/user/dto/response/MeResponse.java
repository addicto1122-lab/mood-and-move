package com.moodandmove.user.dto.response;

import com.moodandmove.user.domain.type.AgeGroup;
import com.moodandmove.user.domain.type.Gender;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        AgeGroup ageGroup,
        Gender gender,
        boolean onboardingCompleted,
        boolean localLoginEnabled
) {
}