package com.moodandmove.calendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalendarDetailResponse(

        Long moodEntryId,

        LocalDate date,

        String emotionCode,
        String emotionName,
        String emoji,

        Integer moodScore,
        Integer intensity,

        String diaryContent,
        String recommendationStatus,
        LocalDateTime recordedAt
) {
}
