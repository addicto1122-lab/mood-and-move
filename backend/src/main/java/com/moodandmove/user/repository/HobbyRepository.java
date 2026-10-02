package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.Hobby;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HobbyRepository extends JpaRepository<Hobby, Long> {

    List<Hobby> findAllByActiveTrueOrderByIdAsc();

    List<Hobby> findAllByIdIn(List<Long> ids);
}