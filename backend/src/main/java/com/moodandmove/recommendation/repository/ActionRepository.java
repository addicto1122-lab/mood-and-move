package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionRepository
        extends JpaRepository<Action, Long> {

    Optional<Action> findByActionCode(String actionCode);

    List<Action> findAllByActiveTrue();

}