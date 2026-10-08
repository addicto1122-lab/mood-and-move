
package com.moodandmove.home.dto.response;

import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.recommendation.domain.entity.Recommendation;

import java.time.LocalDate;

public record HomeResponse(
        LatestMood latestMood,
        TodayRecommendation todayRecommendation
) {

    public record LatestMood(
            Long moodEntryId,
            LocalDate entryDate,
            String emotionCode,
            String emotionName,
            String emoji,
            Integer moodScore,
            Integer intensity
    ) {

        public static LatestMood from(MoodEntry moodEntry) {
            return new LatestMood(
                    moodEntry.getId(),
                    moodEntry.getEntryDate(),
                    moodEntry.getEmotion().getEmotionCode(),
                    moodEntry.getEmotion().getName(),
                    moodEntry.getEmotion().getEmoji(),
                    moodEntry.getMoodScore(),
                    moodEntry.getIntensity()
            );
        }
    }

    public record TodayRecommendation(
            Long recommendationId,
            Long actionId,
            String actionName,
            Integer durationMinutes,
            String reason
    ) {

        public static TodayRecommendation from(
                Recommendation recommendation
        ) {
            return new TodayRecommendation(
                    recommendation.getId(),
                    recommendation.getAction().getId(),
                    recommendation.getAction().getActionName(),
                    recommendation.getAction().getDurationMinutes(),
                    recommendation.getReason()
            );
        }
    }
}
