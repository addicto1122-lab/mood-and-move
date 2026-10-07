package com.moodandmove.recommendation.service;

import com.moodandmove.analysis.dto.ActionAcceptanceStatResponse;
import com.moodandmove.analysis.dto.ActionPersonalStatResponse;
import com.moodandmove.analysis.service.UserActionStatService;
import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.recommendation.domain.dto.ActionCandidateDto;
import com.moodandmove.recommendation.domain.dto.CurrentStateDto;
import com.moodandmove.recommendation.domain.dto.LlmRecommendationRequestDto;
import com.moodandmove.recommendation.domain.dto.UserPreferenceDto;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.recommendation.repository.ActionHobbyRepository;
import com.moodandmove.recommendation.repository.ActionRepository;
import com.moodandmove.recommendation.repository.RecommendationRepository;
import com.moodandmove.recommendation.repository.RecommendationSessionRepository;
import com.moodandmove.user.domain.entity.UserPreference;
import com.moodandmove.user.repository.UserPreferenceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {
    private final ActionRepository actionRepository;
    private final ActionHobbyRepository actionHobbyRepository;
    private final RecommendationSessionRepository recommendationSessionRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final TimeBucketResolver timeBucketResolver;

    private final UserActionStatService userActionStatService;
    private final PersonalScoreCalculator personalScoreCalculator;

    public BigDecimal calculatePersonalScore(
            ActionAcceptanceStatResponse acceptanceStat,
            ActionPersonalStatResponse emotionStat,
            BigDecimal confidenceWeight
    ) {
        BigDecimal acceptanceRate = personalScoreCalculator.calculateRate(
                acceptanceStat.executionCount(),
                acceptanceStat.recommendationCount());

        BigDecimal emotionExecutionRate = personalScoreCalculator.calculateRate(
                emotionStat.executionCount(),
                emotionStat.recommendationCount());

        BigDecimal positiveRate = personalScoreCalculator.calculateRate(
                emotionStat.positiveCount(),
                emotionStat.sampleCount());

        return personalScoreCalculator.calculate(
                emotionStat.avgDelta(),
                acceptanceRate,
                positiveRate,
                emotionExecutionRate,
                confidenceWeight
        );
    }

    public List<ActionCandidateDto> createActionCandidates(
            Long userId, String emotionCode) {

        List<Action> actions = actionRepository.findAllByActiveTrue();
        List<ActionCandidateDto> candidates = new ArrayList<>();

        for (Action action : actions) {
            ActionAcceptanceStatResponse acceptanceStat =
                    userActionStatService.getAcceptanceStat(userId, action.getId());

            ActionPersonalStatResponse emotionStat =
                    userActionStatService.getPersonalStat(userId, action.getId(), emotionCode);

            if (acceptanceStat.recommendationCount() == 0
                    || emotionStat.recommendationCount() == 0
                    || emotionStat.sampleCount() == 0) {
                continue;
            }

            BigDecimal confidenceWeight = personalScoreCalculator.resolveConfidenceWeight(
                    emotionStat.confidenceLevel());

            BigDecimal personalScore = calculatePersonalScore(
                    acceptanceStat,emotionStat, confidenceWeight);

            ActionCandidateDto candidate = new ActionCandidateDto(
                    action.getActionCode(),
                    action.getName(),
                    personalScore);

            candidates.add(candidate);
        }
        return candidates;
    }

    private CurrentStateDto createCurrentState(
            Long userId, Long moodEntryId){
        MoodEntry moodEntry = moodEntryRepository.findByIdAndUser_IdAndDeletedAtIsNull(moodEntryId,userId)
                                                .orElseThrow(()
                                                        -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다."));

        TimeBucket timeBucket = timeBucketResolver.resolve(moodEntry.getRecordedAt());

        return new CurrentStateDto(
                moodEntry.getEmotion().getEmotionCode(),
                moodEntry.getIntensity(),
                moodEntry.getDiaryContent(),
                timeBucket
                );
    }

    private UserPreferenceDto createUserPreference(Long userId){
        UserPreference preference = userPreferenceRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 선호 정보를 찾을 수 없습니다"));

        return new UserPreferenceDto(
                preference.getActivityEnvironment(),
                preference.getActivityStyle(),
                preference.getSocialPreference()
        );
    }

    public LlmRecommendationRequestDto createLlmRequest(
            Long userId,
            Long moodEntryId){


        CurrentStateDto currentState = createCurrentState(userId,moodEntryId);
        UserPreferenceDto preference = createUserPreference(userId);
        List<ActionCandidateDto> candidates = createActionCandidates(userId, currentState.emotion());

        return new LlmRecommendationRequestDto(
                currentState,preference,candidates);
    }
}
