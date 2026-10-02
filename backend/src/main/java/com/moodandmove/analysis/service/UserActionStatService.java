package com.moodandmove.analysis.service;

import com.moodandmove.analysis.domain.entity.UserActionStat;
import com.moodandmove.analysis.dto.ActionPersonalStatResponse;
import com.moodandmove.analysis.dto.EmotionStatResponse;
import com.moodandmove.analysis.dto.MonthlyStatsResponse;
import com.moodandmove.analysis.repository.UserActionStatRepository;
import com.moodandmove.mood.domain.entity.Emotion;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.mood.repository.EmotionRepository;
import com.moodandmove.mood.repository.MoodEntryRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActionStatService {

    private final UserActionStatRepository userActionStatRepository;
    private final MoodEntryRepository moodEntryRepository;

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

        return new ActionPersonalStatResponse(
                actionId,
                emotionCode,
                recommendationCount,
                executionCount,
                sampleCount,
                positiveCount,
                avgDelta
        );
    }

    @Transactional(readOnly = true)
    public MonthlyStatsResponse getMonthlyStats(
            Long userId,
            int year,
            int month
    ){
        List<UserActionStat> stats = userActionStatRepository
                .findAllByUser_IdAndStatYearAndStatMonth(
                        userId,
                        year,
                        month
                );
        long recommendationCount = 0;
        long executionCount = 0;
        long sampleCount = 0;
        long positiveCount = 0;

        BigDecimal totalDelta = BigDecimal.ZERO;

        for(UserActionStat stat : stats)
        {
            recommendationCount += stat.getRecommendationCount();

            executionCount += stat.getExecutionCount();

            sampleCount += stat.getSampleCount();

            positiveCount += stat.getPositiveCount();

            /*
             * avgDelta는 단순 평균하면 안 됨
             *
             * 각 Row의 avgDelta × sampleCount
             */
            if(stat.getSampleCount() > 0 && stat.getAvgDelta() != null)
            {
                totalDelta = totalDelta.add(
                        stat.getAvgDelta().multiply(
                                BigDecimal.valueOf(
                                        stat.getSampleCount()
                                )
                        )
                );
            }
        }

        /*
         * 3. 추천 → 실행 비율
         */
        BigDecimal executionRate;

        if(recommendationCount == 0)
        {
            executionRate = BigDecimal.ZERO;
        }else{
            executionRate = BigDecimal.valueOf(executionCount)
                    .multiply(
                            BigDecimal.valueOf(100)
                    )
                    .divide(
                            BigDecimal.valueOf(
                                    recommendationCount
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        /*
         * 4. 긍정 변화 비율
         */
        BigDecimal positiveRate;

        if(sampleCount == 0)
        {
            positiveRate = BigDecimal.ZERO;
        }else {
            positiveRate = BigDecimal.valueOf(positiveCount)
                    .multiply(
                            BigDecimal.valueOf(100)
                    )
                    .divide(
                            BigDecimal.valueOf(
                                    sampleCount
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        /*
         * 5. 월 평균 변화량
         */
        BigDecimal averageDelta;

        if(sampleCount == 0)
        {
            averageDelta = BigDecimal.ZERO;
        }else {
            averageDelta = totalDelta.divide(
                    BigDecimal.valueOf(sampleCount),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * 6. 해당 월 일기 조회
         */
        LocalDate startDate = LocalDate.of(
                year,
                month,
                1
        );

        LocalDate endDate = startDate.plusMonths(1);

        List<MoodEntry> moodEntries = moodEntryRepository
                .findAllByUser_IdAndEntryDateGreaterThanEqualAndEntryDateLessThanAndDeletedAtIsNullOrderByEntryDateAsc(
                        userId,
                        startDate,
                        endDate
                );

        /*
         * 7. 일기 작성 횟수
         */
        long diaryCount = moodEntries.size();

        /*
         * 8. 월 평균 기분 점수
         */
        BigDecimal averageMoodScore;

        if(moodEntries.isEmpty())
        {
            averageMoodScore = BigDecimal.ZERO;
        }else {
            double average = moodEntries.stream()
                    .mapToInt(
                            MoodEntry::getMoodScore
                    )
                    .average()
                    .orElse(0);

            averageMoodScore = BigDecimal.valueOf(average)
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );


        }
        if(moodEntries.isEmpty())
        {
            averageMoodScore = BigDecimal.ZERO;
        }else{
            double average = moodEntries.stream()
                    .mapToInt(
                            MoodEntry::getMoodScore
                    )
                    .average()
                    .orElse(0);

            averageMoodScore = BigDecimal.valueOf(average)
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        /*
         * 9. 감정별 분포 계산
         */
        Map<String, List<MoodEntry>> grouped = moodEntries.stream()
                .collect(
                        Collectors.groupingBy(
                                entry -> entry.getEmotion()
                                        .getEmotionCode()
                        )
                );

        List<EmotionStatResponse> emotionStats = grouped.values().stream()
                .map(entries -> {
                    MoodEntry firstEntry = entries.get(0);

                    long count = entries.size();

                    BigDecimal rate;

                    if(diaryCount == 0)
                    {
                        rate = BigDecimal.ZERO;
                    }else{
                        rate = BigDecimal.valueOf(count)
                                .multiply(
                                        BigDecimal.valueOf(100)
                                )
                                .divide(
                                        BigDecimal.valueOf(diaryCount),
                                        2,
                                        RoundingMode.HALF_UP
                                );
                    }

                    return new EmotionStatResponse(
                            firstEntry.getEmotion()
                                    .getEmotionCode(),

                            firstEntry.getEmotion()
                                    .getName(),

                            firstEntry.getEmotion()
                                    .getEmoji(),

                            count,

                            rate
                    );
                })
                .toList();



        return new MonthlyStatsResponse(
                year,
                month,

                diaryCount,
                averageMoodScore,

                recommendationCount,
                executionCount,
                sampleCount,
                positiveCount,

                executionRate,
                positiveRate,
                averageDelta,

                emotionStats
        );
    }

}
