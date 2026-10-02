package com.moodandmove.analysis.dto;

import java.math.BigDecimal;

public record EmotionStatResponse (
        String emotionCode,
        String emotionName,
        String emoji,
        Long count,
        BigDecimal rate
){
}
