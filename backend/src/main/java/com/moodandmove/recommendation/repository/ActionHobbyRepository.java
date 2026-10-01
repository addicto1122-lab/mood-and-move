package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.ActionHobby;
import com.moodandmove.recommendation.domain.entity.ActionHobbyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionHobbyRepository
        extends JpaRepository<ActionHobby, ActionHobbyId> {
}