package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.RecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationSessionRepository
        extends JpaRepository<RecommendationSession, Long> {
}