package com.moodandmove.user.dto.response;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        boolean onboardingCompleted
) {
}