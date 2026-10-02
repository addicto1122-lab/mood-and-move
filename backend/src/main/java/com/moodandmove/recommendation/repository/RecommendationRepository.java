package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository
        extends JpaRepository<Recommendation, Long> {
}