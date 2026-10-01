package com.moodandmove.recommendation.domain.dto;

import com.moodandmove.common.domain.type.TimeBucket;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CurrentStateDto {
    private String emotion;
    private Integer intensity;
    private List<String> activityTags;
    private String memo;
    private TimeBucket timeBucket;
}
