package com.moodandmove.recommendation.service;

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
         * 표본 수에 따른 효과 점수 반영 비중
         *
         *  0회 → 기본 50점 유지 (메서드 앞부분에서 처리)
         *  5회 → 약 33% 반영
         * 10회 → 50% 반영
         * 30회 → 75% 반영
         *
         * 통계적 확률이 아니라 점수 보정용 가중치다.
         */
        BigDecimal sampleSize = BigDecimal.valueOf(sampleCount);

        BigDecimal reliability = sampleSize.divide(
                sampleSize.add(BigDecimal.TEN),
                6,
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

}
