package com.moodandmove.recommendation.service;


import com.moodandmove.common.domain.type.TimeBucket;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TimeBucketResolver {
    public TimeBucket resolve(LocalDateTime dateTime) {
        int hour = dateTime.getHour();

        if (hour >= 6 && hour < 11) {
            return TimeBucket.MORNING;
        }
        //if (hour >= 1 && hour < 17) {
        if (hour >= 11 && hour < 17){
            return TimeBucket.AFTERNOON;
        }
        if (hour >= 17 && hour < 21) {
            return TimeBucket.EVENING;
        }
        return TimeBucket.NIGHT;

    }
}