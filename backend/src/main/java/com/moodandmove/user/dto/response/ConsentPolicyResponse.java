package com.moodandmove.user.dto.response;

public record ConsentPolicyResponse(
        Long id,
        String consentType,
        String version,
        String title,
        String content,
        boolean required
) {
}