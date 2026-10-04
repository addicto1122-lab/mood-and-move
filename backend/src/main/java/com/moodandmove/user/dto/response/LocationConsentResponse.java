package com.moodandmove.user.dto.response;

public record LocationConsentResponse(
        Long policyId,
        String version,
        String title,
        String content,
        boolean required,
        boolean agreed
) {
}