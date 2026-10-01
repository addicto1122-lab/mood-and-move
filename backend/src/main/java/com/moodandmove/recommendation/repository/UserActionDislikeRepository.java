package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.UserActionDislike;
import com.moodandmove.recommendation.domain.entity.UserActionDislikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionDislikeRepository
        extends JpaRepository<UserActionDislike, UserActionDislikeId> {
}