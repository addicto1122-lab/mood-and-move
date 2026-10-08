package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.Recommendation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository
        extends JpaRepository<Recommendation, Long> {

    @EntityGraph(attributePaths = "action")
    List<Recommendation> findAllBySession_IdOrderByRankNoAsc(Long sessionId);

    @EntityGraph(attributePaths = "action")
    Optional<Recommendation> findFirstBySession_MoodEntry_IdOrderByRankNoAsc(Long moodEntryId);
}