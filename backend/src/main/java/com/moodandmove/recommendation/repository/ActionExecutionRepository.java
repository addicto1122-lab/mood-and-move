package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.ActionExecution;
import com.moodandmove.recommendation.domain.type.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActionExecutionRepository
        extends JpaRepository<ActionExecution, Long> {

    // 해당 추천 세션에서 이미 선택한 행동이 있는지 조회
    Optional<ActionExecution> findBySession_Id(Long session_id);

    // 로그인한 사용자 소유의 실행 기록 조회
    Optional<ActionExecution> findByIdAndUserId(Long executionId, Long userId);

    // 최근 일기에 연결된 최근 진행 중 행동 조회
    Optional<ActionExecution> findFirstByUserIdAndStatusAndSession_MoodEntry_DeletedAtIsNullOrderByStartedAtDescIdDesc(
            Long userId, ExecutionStatus status);
}