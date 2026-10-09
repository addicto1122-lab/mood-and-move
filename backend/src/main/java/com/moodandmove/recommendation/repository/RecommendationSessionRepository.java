package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.RecommendationSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationSessionRepository
        extends JpaRepository<RecommendationSession, Long> {

    Optional<RecommendationSession> findByMoodEntry_Id(Long moodEntryId);

    //로그인한 사용자 소유의 추천 세션과 연결된 일기 조회
    @EntityGraph(attributePaths = "moodEntry")
    Optional<RecommendationSession> findByIdAndUserId(Long sessionId, Long userId);
}