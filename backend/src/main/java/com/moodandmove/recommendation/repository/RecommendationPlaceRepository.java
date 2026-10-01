package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.RecommendationPlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationPlaceRepository
        extends JpaRepository<RecommendationPlace, Long> {
}