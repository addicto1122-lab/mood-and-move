package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.RecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationSessionRepository
        extends JpaRepository<RecommendationSession, Long> {

    Optional<RecommendationSession> findByMoodEntry_Id(Long moodEntryId);
}