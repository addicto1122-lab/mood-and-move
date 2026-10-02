package com.moodandmove.mood.repository;

import com.moodandmove.mood.domain.entity.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmotionRepository
        extends JpaRepository<Emotion, String> {

    List<Emotion> findAllByActiveTrue();
}