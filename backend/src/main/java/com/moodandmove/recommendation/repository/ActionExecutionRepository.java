package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.ActionExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionExecutionRepository
        extends JpaRepository<ActionExecution, Long> {
}