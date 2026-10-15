package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;

public record LlmRecommendedActionDto(
        String actionName,
        String category,
        Integer durationMinutes,
        Integer durationDays,
        Integer durationNights,
        EnvironmentType environmentType,
        SocialType socialType,
        ActivityStyle activityStyle,
        boolean locationRequired,
        String placeCategory,
        String placeKeyword,
        String reason,
        String emoji
) {

    public boolean hasValidDuration() {
        boolean minuteBased =
                durationMinutes != null
                        && durationMinutes > 0
                        && durationDays == null
                        && durationNights == null;

        boolean dayBased =
                durationMinutes == null
                        && durationDays != null
                        && durationDays >= 1
                        && durationNights != null
                        && durationNights >= 0
                        && durationNights < durationDays;

        return minuteBased || dayBased;
    }

    // JSON 응답을 DTO로 변환할 때 장소 필드 정리
    public LlmRecommendedActionDto {
        placeCategory = normalizeNullablePlaceField(placeCategory);
        placeKeyword = normalizeNullablePlaceField(placeKeyword);
    }

    private static String normalizeNullablePlaceField(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.isEmpty()
                || "null".equalsIgnoreCase(normalized)) {
            return null;
        }

        return normalized;
    }
}