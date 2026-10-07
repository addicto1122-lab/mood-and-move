package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.TimeBucket;


public record CurrentStateDto(
        String emotion,
        Integer intensity,
        Integer moodScore,
        String currentActivity,
        String diaryContent,
        TimeBucket timeBucket
//        String emotion,
//        Integer intensity,
//        String memo,
//        TimeBucket timeBucket

) {

}
