package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.UserHobby;
import com.moodandmove.user.domain.entity.UserHobbyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserHobbyRepository
        extends JpaRepository<UserHobby, UserHobbyId> {

    List<UserHobby> findAllByUser_Id(Long userId);

    void deleteAllByUser_Id(Long userId);
}