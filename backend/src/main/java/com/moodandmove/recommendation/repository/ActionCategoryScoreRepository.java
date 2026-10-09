package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.ActionCategoryScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionCategoryScoreRepository extends JpaRepository<ActionCategoryScore, Long> {
    //사용자의 특정 카테고리 점수 조회
    Optional<ActionCategoryScore> findByUserIdAndCategory(
            Long userId, String category);

    //사용자의 전체 카테고리 점수 조회
    List<ActionCategoryScore> findAllByUserId(Long userId);
}
