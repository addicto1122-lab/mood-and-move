package com.moodandmove.calendar.dto;

import java.time.LocalDate;

public record CalendarDayResponse(
        Long moodEntryId,
        LocalDate date,

        String emotionCode,
        String emotionName,
        String emoji,

        Integer moodScore,
        Integer intensity,
        Integer afterScore
){}
