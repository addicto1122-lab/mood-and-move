package com.moodandmove.analysis.repository;

import com.moodandmove.analysis.domain.entity.UserActionStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserActionStatRepository extends JpaRepository<UserActionStat, Long> {
    List<UserActionStat> findAllByUser_Id(
            Long userId
    );

    List<UserActionStat> findAllByUser_IdAndEmotion_EmotionCode(
            Long userId,
            String emotionCode
    );

    List<UserActionStat> findAllByUser_IdAndAction_Id(
            Long userId,
            Long actionId
    );

    Optional<UserActionStat> findByUser_IdAndAction_IdAndEmotion_EmotionCodeAndStatYearAndStatMonth
            (
                Long userId,
                Long actionId,
                String emotionCode,
                Integer statYear,
                Integer statMonth
            );

    List<UserActionStat> findAllByUser_IdAndAction_IdAndEmotion_EmotionCode
            (
                Long userId,
                Long actionId,
                String emotionCode
            );

    List<UserActionStat> findAllByUser_IdAndStatYearAndStatMonth
            (
                Long userId,
                Integer statYear,
                Integer statMonth
            );
}
