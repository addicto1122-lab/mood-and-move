package com.moodandmove.mood.domain.type;

public record MoodTrendResponse (
        String date,
        Integer beforeScore,
        Integer afterScore
){
}
