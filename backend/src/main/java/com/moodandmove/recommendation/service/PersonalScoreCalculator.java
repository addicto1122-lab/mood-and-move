package com.moodandmove.recommendation.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PersonalScoreCalculator {
    private static final BigDecimal MAX_DELTA = BigDecimal.valueOf(59);

    private static final BigDecimal DELTA_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal ACCEPTANCE_WEIGHT = BigDecimal.valueOf(0.20);
    private static final BigDecimal POSITIVE_WEIGHT = BigDecimal.valueOf(0.25);
    private static final BigDecimal EMOTION_EXECUTION_WEIGHT = BigDecimal.valueOf(0.20);


    public BigDecimal calculate(
            BigDecimal avgDelta,
            BigDecimal acceptanceRate,
            BigDecimal positiveRate,
            BigDecimal emotionExecutionRate,
            BigDecimal confidenceWeight
    ) {
        BigDecimal deltaScore = calculateDeltaScore(avgDelta);

        BigDecimal score = deltaScore.multiply(DELTA_WEIGHT)
                            .add(acceptanceRate.multiply(ACCEPTANCE_WEIGHT))
                            .add(positiveRate.multiply(POSITIVE_WEIGHT))
                            .add(emotionExecutionRate.multiply(EMOTION_EXECUTION_WEIGHT));

        return score.multiply(confidenceWeight).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDeltaScore(BigDecimal avgDelta) {
        BigDecimal deltaScore = BigDecimal.valueOf(50)
                        .add(avgDelta.divide(MAX_DELTA,10,RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(50)));

                        return clamp(deltaScore);
    }
    public BigDecimal calculateRate(int numerator, int denominator) {

        if (denominator <= 0) {
            throw new IllegalArgumentException("분모는 0보다 커야 합니다.");
        }

        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(BigDecimal value) {

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

}
