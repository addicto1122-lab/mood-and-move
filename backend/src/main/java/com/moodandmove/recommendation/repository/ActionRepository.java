package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findByActiveTrue();
}
