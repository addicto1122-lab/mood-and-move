
package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActionRepository
        extends JpaRepository<Action, Long> {

    // 행동 이름으로 조회 (LLM 생성 행동 중복 확인)
    Optional<Action> findByActionName(String actionName);

    // 행동 이름 존재 여부 확인
    boolean existsByActionName(String actionName);

}
