package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.TimeBucket;

import java.util.List;


public record CurrentStateDto(
        String emotion,
        Integer intensity,
        List<String> activityTags,
        String memo,
        TimeBucket timeBucket

) {

}
