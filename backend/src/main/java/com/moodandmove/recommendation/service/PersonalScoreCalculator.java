package com.moodandmove.recommendation.service;

import com.moodandmove.analysis.domain.type.ConfidenceLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PersonalScoreCalculator {

    private static final BigDecimal MAX_DELTA = BigDecimal.valueOf(59);
    private static final BigDecimal NEUTRAL_SCORE = BigDecimal.valueOf(50);
    private static final BigDecimal DELTA_WEIGHT = BigDecimal.valueOf(0.60);
    private static final BigDecimal POSITIVE_RATE_WEIGHT = BigDecimal.valueOf(0.40);

    /*
     * 개인화 점수
     *
     * 데이터 없음   → 50점
     * 데이터 적음   → 50점에 가깝게
     * 데이터 많음   → 실제 행동 효과 점수 적극 반영
     */
    public BigDecimal calculate(
            BigDecimal avgDelta,
            Long positiveCount,
            Long sampleCount
    )
    {
        if(sampleCount <= 0)
        {
            return NEUTRAL_SCORE.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        BigDecimal deltaScore = calculateDeltaScore(avgDelta);

        BigDecimal positiveRate = calculateRate(
                positiveCount,
                sampleCount
        );

        /*
         * 실제 효과 점수
         */
        BigDecimal effectScore = deltaScore.multiply(DELTA_WEIGHT)
                .add(
                        positiveRate.multiply(
                                POSITIVE_RATE_WEIGHT
                        )
                );

        /*
         * 표본 신뢰도
         *
         * 1회  = 0.1
         * 5회  = 0.5
         * 10회 = 1.0
         */
        BigDecimal reliability = BigDecimal.valueOf(
                Math.min(sampleCount, 10)

        ).divide(
                BigDecimal.TEN,
                4,
                RoundingMode.HALF_UP
        );

        /*
         * 표본이 적으면 50점에 가깝게,
         * 많아질수록 실제 효과점수 반영
         */
        BigDecimal score = NEUTRAL_SCORE.multiply(
                BigDecimal.ONE.subtract(reliability)
        )
                .add(
                        effectScore.multiply(reliability)
                );

        return clamp(score)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * avgDelta
     *
     * -59 → 0
     *   0 → 50
     * +59 → 100
     */
    public BigDecimal calculateDeltaScore(
            BigDecimal avgDelta
    )
    {
        if(avgDelta == null)
        {
            return NEUTRAL_SCORE;
        }

        BigDecimal deltaScore = BigDecimal.valueOf(50)
                .add(
                        avgDelta.divide(
                                MAX_DELTA,
                                10,
                                RoundingMode.HALF_UP
                        )
                                .multiply(
                                        BigDecimal.valueOf(50)
                                )
                );

        return clamp(deltaScore);
    }

    public BigDecimal calculateRate(
            Long numerator,
            Long denominator
    )
    {
        if(denominator == null || denominator <= 0)
        {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return BigDecimal.valueOf(numerator)
                .divide(
                        BigDecimal.valueOf(denominator),
                        10,
                        RoundingMode.HALF_UP
                )
                .multiply(
                        BigDecimal.valueOf(100)
                )
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal clamp(
            BigDecimal value
    )
    {
        if(value.compareTo(BigDecimal.ZERO) < 0)
        {
            return BigDecimal.ZERO;
        }

        if(value.compareTo(
                BigDecimal.valueOf(100))> 0)
        {
            return BigDecimal.valueOf(100);
        }

        return value;
    }

//    private static final BigDecimal MAX_DELTA = BigDecimal.valueOf(59);
//
//    private static final BigDecimal DELTA_WEIGHT = BigDecimal.valueOf(0.35);
//    private static final BigDecimal ACCEPTANCE_WEIGHT = BigDecimal.valueOf(0.20);
//    private static final BigDecimal POSITIVE_WEIGHT = BigDecimal.valueOf(0.25);
//    private static final BigDecimal EMOTION_EXECUTION_WEIGHT = BigDecimal.valueOf(0.20);
//
//
//    public BigDecimal calculate(
//            BigDecimal avgDelta,
//            BigDecimal acceptanceRate,
//            BigDecimal positiveRate,
//            BigDecimal emotionExecutionRate,
//            BigDecimal confidenceWeight
//    ) {
//        BigDecimal deltaScore = calculateDeltaScore(avgDelta);
//
//        BigDecimal score = deltaScore.multiply(DELTA_WEIGHT)
//                            .add(acceptanceRate.multiply(ACCEPTANCE_WEIGHT))
//                            .add(positiveRate.multiply(POSITIVE_WEIGHT))
//                            .add(emotionExecutionRate.multiply(EMOTION_EXECUTION_WEIGHT));
//
//        return score.multiply(confidenceWeight).setScale(2, RoundingMode.HALF_UP);
//    }
//
//    public BigDecimal calculateDeltaScore(BigDecimal avgDelta) {
//        BigDecimal deltaScore = BigDecimal.valueOf(50)
//                        .add(avgDelta.divide(MAX_DELTA,10,RoundingMode.HALF_UP)
//                        .multiply(BigDecimal.valueOf(50)));
//
//                        return clamp(deltaScore);
//    }
//    public BigDecimal calculateRate(long numerator, long denominator) {
//
//        if (denominator <= 0) {
//            throw new IllegalArgumentException("분모는 0보다 커야 합니다.");
//        }
//
//        return BigDecimal.valueOf(numerator)
//                .divide(BigDecimal.valueOf(denominator), 10, RoundingMode.HALF_UP)
//                .multiply(BigDecimal.valueOf(100))
//                .setScale(2, RoundingMode.HALF_UP);
//    }
//
//    private BigDecimal clamp(BigDecimal value) {
//
//        if (value.compareTo(BigDecimal.ZERO) < 0) {
//            return BigDecimal.ZERO;
//        }
//
//        if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
//            return BigDecimal.valueOf(100);
//        }
//
//        return value.setScale(2, RoundingMode.HALF_UP);
//    }
//
//    public BigDecimal resolveConfidenceWeight(ConfidenceLevel confidenceLevel){
//        return switch (confidenceLevel){
//            case LOW -> BigDecimal.valueOf(0.5);
//            case MEDIUM -> BigDecimal.valueOf(0.75);
//            case HIGH -> BigDecimal.ONE;
//        };
//    }

}
