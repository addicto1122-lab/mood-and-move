package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.UserActionDislike;
import com.moodandmove.recommendation.domain.entity.UserActionDislikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserActionDislikeRepository
        extends JpaRepository<UserActionDislike, UserActionDislikeId> {

    List<UserActionDislike> findAllByUser_Id(Long userId);

    boolean existsByUser_IdAndAction_Id(
            Long userId,
            Long actionId
    );

    void deleteByUser_IdAndAction_Id(
            Long userId,
            Long actionId
    );
}