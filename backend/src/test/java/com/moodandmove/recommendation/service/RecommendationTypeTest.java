package com.moodandmove.recommendation.service;

import com.moodandmove.recommendation.domain.type.RecommendationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationTypeTest {

    @Test
    void zeroAndThreeSamplesAreColdStart() {
        assertEquals(
                RecommendationType.COLD_START,
                RecommendationService.resolveRecommendationType(0)
        );

        assertEquals(
                RecommendationType.COLD_START,
                RecommendationService.resolveRecommendationType(3)
        );
    }

    @Test
    void fourAndTenSamplesAreHybrid() {
        assertEquals(
                RecommendationType.HYBRID,
                RecommendationService.resolveRecommendationType(4)
        );

        assertEquals(
                RecommendationType.HYBRID,
                RecommendationService.resolveRecommendationType(10)
        );
    }

    @Test
    void elevenOrMoreSamplesArePersonalized() {
        assertEquals(
                RecommendationType.PERSONALIZED,
                RecommendationService.resolveRecommendationType(11)
        );

        assertEquals(
                RecommendationType.PERSONALIZED,
                RecommendationService.resolveRecommendationType(20)
        );
    }
}