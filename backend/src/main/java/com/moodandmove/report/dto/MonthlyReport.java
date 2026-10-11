package com.moodandmove.report.dto;

import java.math.BigDecimal;

public record MonthlyReport (
        int year,
        int month,

        Long diaryCount,
        BigDecimal averageMoodScore,

        String topEmotionName,
        String topEmotionEmoji,

        Long executionCount,
        BigDecimal executionRate,

        BigDecimal positiveRate,
        BigDecimal averageDelta,

        String bestActionName,
        Integer bestActionDelta,

        String mostActionCategory,
        Long mostActiveCount,

        String encouragementMessage
){
}
