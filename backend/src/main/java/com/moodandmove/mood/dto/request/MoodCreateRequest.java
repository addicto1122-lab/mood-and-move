package com.moodandmove.mood.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MoodCreateRequest(

        @NotBlank
        String emotionCode,

        @NotNull
        @Min(1)
        @Max(10)
        Integer intensity,

        @NotBlank
        @Size(max = 100)
        String currentActivity,

        @Size(max = 500)
        String diaryContent
) {
}