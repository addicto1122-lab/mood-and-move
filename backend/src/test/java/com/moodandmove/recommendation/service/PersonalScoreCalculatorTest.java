package com.moodandmove.recommendation.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PersonalScoreCalculatorTest {

    private final PersonalScoreCalculator calculator
            = new PersonalScoreCalculator();



    @Test
    void calculate() {
        BigDecimal result = calculator.calculate(
                BigDecimal.valueOf(10),   // avgDelta
                BigDecimal.valueOf(70),   // acceptanceRate
                BigDecimal.valueOf(80),   // positiveRate
                BigDecimal.valueOf(60),   // emotionExecutionRate
                BigDecimal.valueOf(0.8)   // 테스트용 confidenceWeight
        );

        System.out.println("PersonalScore = " + result);
    }

    @Test
    void calculateDeltaScore() {
        BigDecimal result =
                calculator.calculateDeltaScore(BigDecimal.ZERO);

        assertEquals(
                0,
                result.compareTo(new BigDecimal("50.00"))
        );
    }

    @Test
    void calculateRate() {
        BigDecimal result =
                calculator.calculateRate(7, 10);

        assertEquals(
                0,
                result.compareTo(new BigDecimal("70.00"))
        );
    }

    @Test
    void calculateRateZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateRate(1, 0)
        );
    }
}