package com.moodandmove.analysis.service;

import com.moodandmove.analysis.domain.entity.UserActionStat;
import com.moodandmove.analysis.domain.type.ConfidenceLevel;
import com.moodandmove.analysis.dto.ActionAcceptanceStatResponse;
import com.moodandmove.analysis.dto.ActionPersonalStatResponse;
import com.moodandmove.analysis.repository.UserActionStatRepository;
import com.moodandmove.mood.domain.entity.Emotion;
import com.moodandmove.mood.repository.EmotionRepository;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.recommendation.repository.ActionRepository;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserActionStatService {

    private final UserActionStatRepository userActionStatRepository;

    private final UserRepository userRepository;
    private final ActionRepository actionRepository;
    private final EmotionRepository emotionRepository;


    /*
     * 추천 발생
     */
    @Transactional
    public void recordRecommendation(
            Long userId,
            Long actionId,
            String emotionCode,
            LocalDate entryDate
    ) {

        UserActionStat stat =
                getOrCreate(
                        userId,
                        actionId,
                        emotionCode,
                        entryDate
                );

        stat.increaseRecommendationCount();
    }


    /*
     * 행동 실행
     */
    @Transactional
    public void recordExecution(
            Long userId,
            Long actionId,
            String emotionCode,
            LocalDate entryDate
    ) {

        UserActionStat stat =
                getOrCreate(
                        userId,
                        actionId,
                        emotionCode,
                        entryDate
                );

        stat.increaseExecutionCount();
    }


    /*
     * 재측정 완료
     */
    @Transactional
    public void recordRecheck(
            Long userId,
            Long actionId,
            String emotionCode,
            LocalDate entryDate,
            int delta
    ) {

        UserActionStat stat =
                getOrCreate(
                        userId,
                        actionId,
                        emotionCode,
                        entryDate
                );

        stat.recordRecheck(delta);
    }

    private UserActionStat getOrCreate(
            Long userId,
            Long actionId,
            String emotionCode,
            LocalDate entryDate
    ) {

        int year = entryDate.getYear();
        int month = entryDate.getMonthValue();

        return userActionStatRepository
                .findByUser_IdAndAction_IdAndEmotion_EmotionCodeAndStatYearAndStatMonth(
                        userId,
                        actionId,
                        emotionCode,
                        year,
                        month
                )
                .orElseGet(() -> {

                    User user =
                            userRepository.findById(userId)
                                    .orElseThrow(
                                            () -> new IllegalArgumentException(
                                                    "사용자를 찾을 수 없습니다."
                                            )
                                    );

                    Action action =
                            actionRepository.findById(actionId)
                                    .orElseThrow(
                                            () -> new IllegalArgumentException(
                                                    "행동을 찾을 수 없습니다."
                                            )
                                    );

                    Emotion emotion =
                            emotionRepository.findById(emotionCode)
                                    .orElseThrow(
                                            () -> new IllegalArgumentException(
                                                    "감정을 찾을 수 없습니다."
                                            )
                                    );

                    UserActionStat newStat =
                            UserActionStat.create(
                                    user,
                                    action,
                                    emotion,
                                    year,
                                    month
                            );

                    return userActionStatRepository.save(newStat);
                });
    }

    @Transactional(readOnly = true)
    public ActionPersonalStatResponse getPersonalStat(
            Long userId,
            Long actionId,
            String emotionCode
    ) {

        List<UserActionStat> stats =
                userActionStatRepository
                        .findAllByUser_IdAndAction_IdAndEmotion_EmotionCode(
                                userId,
                                actionId,
                                emotionCode
                        );

        long recommendationCount = 0;
        long executionCount = 0;
        long sampleCount = 0;
        long positiveCount = 0;

        BigDecimal weightedDeltaSum = BigDecimal.ZERO;

        for (UserActionStat stat : stats) {

            recommendationCount +=
                    stat.getRecommendationCount();

            executionCount +=
                    stat.getExecutionCount();

            sampleCount +=
                    stat.getSampleCount();

            positiveCount +=
                    stat.getPositiveCount();

            if (stat.getSampleCount() > 0
                    && stat.getAvgDelta() != null) {

                weightedDeltaSum =
                        weightedDeltaSum.add(
                                stat.getAvgDelta()
                                        .multiply(
                                                BigDecimal.valueOf(
                                                        stat.getSampleCount()
                                                )
                                        )
                        );
            }
        }

        BigDecimal avgDelta;

        if (sampleCount == 0) {

            avgDelta = BigDecimal.ZERO;

        } else {

            avgDelta =
                    weightedDeltaSum.divide(
                            BigDecimal.valueOf(sampleCount),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        ConfidenceLevel confidenceLevel =
                calculateConfidence(sampleCount);

        return new ActionPersonalStatResponse(
                actionId,
                emotionCode,
                recommendationCount,
                executionCount,
                sampleCount,
                positiveCount,
                avgDelta,
                confidenceLevel
        );
    }
    @Transactional(readOnly = true)
    public ActionAcceptanceStatResponse getAcceptanceStat(
            Long userId,
            Long actionId
    ) {
        List<UserActionStat> stats =
                userActionStatRepository
                        .findAllByUser_IdAndAction_Id(
                                userId,
                                actionId
                        );

        long recommendationCount = 0;
        long executionCount = 0;

        for (UserActionStat stat : stats) {
            recommendationCount += stat.getRecommendationCount();
            executionCount += stat.getExecutionCount();
        }

        return new ActionAcceptanceStatResponse(
                actionId,
                recommendationCount,
                executionCount
        );
    }
    private ConfidenceLevel calculateConfidence(long sampleCount) {
        if (sampleCount >= 10) {
            return ConfidenceLevel.HIGH;
        }
        if (sampleCount >= 5) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.LOW;
    }
}
