package com.moodandmove.mood.service;

import com.moodandmove.mood.domain.entity.Emotion;
import org.springframework.stereotype.Component;

@Component
public class MoodScoreCalculator {

    public int calculate(
            Emotion emotion,
            int intensity
    ){
        if(intensity < 1 || intensity > 10)
        {
            throw new IllegalArgumentException(
                    "감정 강도는 1~10 사이여야 합니다."
            );
        }

        int baseScore = emotion.getBaseScore();
        String emotionCode = emotion.getEmotionCode();

        return switch(emotionCode)
        {
            // 강할수록 기분이 나쁨
            case "ANGRY", "ANXIOUS", "SAD" ->
                baseScore + (11 - intensity);

            // 보통은 중앙값 사용
            case "NEUTRAL" ->
                35;

            // 강할수록 기분이 좋음
            case "CALM", "JOY" ->
                baseScore + intensity;

            default ->
                throw new IllegalArgumentException(
                        "지원하지 않는 감정입니다: " + emotionCode
                );
        };
    }

}
