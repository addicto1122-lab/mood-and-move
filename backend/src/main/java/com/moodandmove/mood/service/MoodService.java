package com.moodandmove.mood.service;

import com.moodandmove.mood.domain.entity.Emotion;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.mood.dto.request.MoodCreateRequest;
import com.moodandmove.mood.repository.EmotionRepository;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.recommendation.domain.type.ExecutionStatus;
import com.moodandmove.recommendation.repository.ActionExecutionRepository;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MoodService {

    private final MoodEntryRepository moodEntryRepository;
    private final EmotionRepository emotionRepository;
    private final UserRepository userRepository;
    private final MoodScoreCalculator moodScoreCalculator;
    private final ActionExecutionRepository actionExecutionRepository;

    @Transactional
    public Long createMood(
            Long userId,
            MoodCreateRequest request
    ) {

        boolean hasPendingRecheck = actionExecutionRepository.findFirstByUserIdAndStatusAndSession_MoodEntry_DeletedAtIsNullOrderByStartedAtDescIdDesc(
                userId,
                ExecutionStatus.STARTED
        )
                .isPresent();

        if(hasPendingRecheck){
            throw new IllegalStateException(
                    "이전 행동의 기분 재측정을 먼저 완료해주세요."
            );
        }

        LocalDate today = LocalDate.now();

        if (moodEntryRepository.existsByUser_IdAndEntryDate(userId, today)) {
            throw new IllegalArgumentException(
                    "오늘의 감정 기록은 이미 작성했습니다."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        Emotion emotion = emotionRepository
                .findById(request.emotionCode())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 감정입니다."
                        )
                );

        if (!emotion.isActive()) {
            throw new IllegalArgumentException(
                    "현재 사용할 수 없는 감정입니다."
            );
        }

        int moodScore =
                moodScoreCalculator.calculate(
                        emotion,
                        request.intensity()
                );

        MoodEntry moodEntry = MoodEntry.create(
                user,
                emotion,
                request.intensity(),
                moodScore,
                request.currentActivity(),
                request.diaryContent()
        );

        MoodEntry savedMood =
                moodEntryRepository.save(moodEntry);

        return savedMood.getId();
    }

    @Transactional(readOnly = true)
    public boolean hasTodayMood(Long userId)
    {
        LocalDate today = LocalDate.now();

        return moodEntryRepository.existsByUser_IdAndEntryDate(
                userId,
                today
        );
    }
}