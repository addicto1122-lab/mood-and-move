package com.moodandmove.recommendation.service;

import com.moodandmove.analysis.dto.ActionPersonalStatResponse;
import com.moodandmove.analysis.service.UserActionStatService;
import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.recommendation.domain.dto.*;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.recommendation.llm.RecommendationLlmGenerator;
import com.moodandmove.recommendation.repository.ActionHobbyRepository;
import com.moodandmove.recommendation.repository.ActionRepository;
import com.moodandmove.recommendation.repository.RecommendationRepository;
import com.moodandmove.recommendation.repository.RecommendationSessionRepository;
import com.moodandmove.user.domain.entity.UserPreference;
import com.moodandmove.user.repository.UserPreferenceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.moodandmove.place.domain.type.LocationMode;

import com.moodandmove.recommendation.domain.dto.RecommendationGenerateRequest;
import com.moodandmove.recommendation.domain.dto.RecommendationLocationDto;

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
    private final RecommendationLlmGenerator recommendationLlmGenerator;

    public BigDecimal calculatePersonalScore(
            ActionPersonalStatResponse emotionStat
    )
    {
        return personalScoreCalculator.calculate(
                emotionStat.avgDelta(),
                emotionStat.positiveCount(),
                emotionStat.sampleCount()
        );

    }
//    public BigDecimal calculatePersonalScore(
//            ActionAcceptanceStatResponse acceptanceStat,
//            ActionPersonalStatResponse emotionStat,
//            BigDecimal confidenceWeight
//    ) {
//        BigDecimal acceptanceRate = personalScoreCalculator.calculateRate(
//                acceptanceStat.executionCount(),
//                acceptanceStat.recommendationCount());
//
//        BigDecimal emotionExecutionRate = personalScoreCalculator.calculateRate(
//                emotionStat.executionCount(),
//                emotionStat.recommendationCount());
//
//        BigDecimal positiveRate = personalScoreCalculator.calculateRate(
//                emotionStat.positiveCount(),
//                emotionStat.sampleCount());
//
//        return personalScoreCalculator.calculate(
//                emotionStat.avgDelta(),
//                acceptanceRate,
//                positiveRate,
//                emotionExecutionRate,
//                confidenceWeight
//        );
//    }

    public List<ActionCandidateDto> createActionCandidates(
            Long userId,
            String emotionCode,
            LocationMode locationMode
    )
    {
        List<Action> actions = actionRepository.findAllByActiveTrue();

        List<ActionCandidateDto> candidates = new ArrayList<>();

        for (Action action : actions) {

            /*
             * 위치 사용 안 함인데
             * 위치 필수 행동이면 후보 제외
             */
            if (locationMode == LocationMode.NONE
                    && action.isLocationRequired()) {

                continue;
            }


            ActionPersonalStatResponse emotionStat =
                    userActionStatService.getPersonalStat(
                            userId,
                            action.getId(),
                            emotionCode
                    );


            BigDecimal personalScore =
                    calculatePersonalScore(
                            emotionStat
                    );


            candidates.add(
                    new ActionCandidateDto(
                            action.getActionCode(),
                            action.getName(),
                            personalScore
                    )
            );
        }

        return candidates;
    }

//    public List<ActionCandidateDto> createActionCandidates(
//            Long userId, String emotionCode) {
//
//        List<Action> actions = actionRepository.findAllByActiveTrue();
//        List<ActionCandidateDto> candidates = new ArrayList<>();
//
//        for (Action action : actions) {
//            ActionAcceptanceStatResponse acceptanceStat =
//                    userActionStatService.getAcceptanceStat(userId, action.getId());
//
//            ActionPersonalStatResponse emotionStat =
//                    userActionStatService.getPersonalStat(userId, action.getId(), emotionCode);
//
//            if (acceptanceStat.recommendationCount() == 0
//                    || emotionStat.recommendationCount() == 0
//                    || emotionStat.sampleCount() == 0) {
//                continue;
//            }
//
//            BigDecimal confidenceWeight = personalScoreCalculator.resolveConfidenceWeight(
//                    emotionStat.confidenceLevel());
//
//            BigDecimal personalScore = calculatePersonalScore(
//                    acceptanceStat,emotionStat, confidenceWeight);
//
//            ActionCandidateDto candidate = new ActionCandidateDto(
//                    action.getActionCode(),
//                    action.getName(),
//                    personalScore);
//
//            candidates.add(candidate);
//        }
//        return candidates;
//    }

    private CurrentStateDto createCurrentState(
            Long userId,
            Long moodEntryId
    ) {
        MoodEntry moodEntry =
                moodEntryRepository
                        .findByIdAndUser_IdAndDeletedAtIsNull(
                                moodEntryId,
                                userId
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "감정 기록을 찾을 수 없습니다."
                                )
                        );

        TimeBucket timeBucket =
                timeBucketResolver.resolve(
                        moodEntry.getRecordedAt()
                );

        return new CurrentStateDto(
                moodEntry.getEmotion().getEmotionCode(),
                moodEntry.getIntensity(),
                moodEntry.getMoodScore(),
                moodEntry.getCurrentActivity(),   // 추가
                moodEntry.getDiaryContent(),
                timeBucket
        );
    }

//    private CurrentStateDto createCurrentState(
//            Long userId, Long moodEntryId){
//        MoodEntry moodEntry = moodEntryRepository.findByIdAndUser_IdAndDeletedAtIsNull(moodEntryId,userId)
//                                                .orElseThrow(()
//                                                        -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다."));
//
//        TimeBucket timeBucket = timeBucketResolver.resolve(moodEntry.getRecordedAt());
//
//        return new CurrentStateDto(
//                moodEntry.getEmotion().getEmotionCode(),
//                moodEntry.getIntensity(),
//                moodEntry.getDiaryContent(),
//                timeBucket
//                );
//    }

    private UserPreferenceDto createUserPreference(
            Long userId
    ) {

        return userPreferenceRepository
                .findByUser_Id(userId)
                .map(preference ->
                        new UserPreferenceDto(
                                preference.getActivityEnvironment(),
                                preference.getActivityStyle(),
                                preference.getSocialPreference()
                        )
                )
                .orElse(null);
    }

//    private UserPreferenceDto createUserPreference(Long userId){
//        UserPreference preference = userPreferenceRepository.findByUser_Id(userId)
//                .orElseThrow(() -> new IllegalArgumentException("사용자 선호 정보를 찾을 수 없습니다"));
//
//        return new UserPreferenceDto(
//                preference.getActivityEnvironment(),
//                preference.getActivityStyle(),
//                preference.getSocialPreference()
//        );
//    }

    public LlmRecommendationRequestDto createLlmRequest(
            Long userId,
            Long moodEntryId,
            RecommendationGenerateRequest request
    ) {

        // 1. 일기에서 현재 상태 조회
        CurrentStateDto currentState =
                createCurrentState(
                        userId,
                        moodEntryId
                );

        // 2. 사용자가 선택한 위치 처리
        RecommendationLocationDto location =
                resolveLocation(
                        userId,
                        request
                );

        // 3. 온보딩 선호 조회
        // 없으면 null이어도 추천 진행
        UserPreferenceDto preference =
                createUserPreference(userId);

        // 4. 추천 가능한 행동 후보 조회
        List<ActionCandidateDto> candidates =
                createActionCandidates(
                        userId,
                        currentState.emotion(),
                        location.locationMode()
                );

        // 5. LLM에 전달할 최종 데이터
        return new LlmRecommendationRequestDto(
                currentState,
                location,
                preference,
                candidates
        );
    }

//    public LlmRecommendationRequestDto createLlmRequest(
//            Long userId,
//            Long moodEntryId){
//
//
//        CurrentStateDto currentState = createCurrentState(userId,moodEntryId);
//        UserPreferenceDto preference = createUserPreference(userId);
//        List<ActionCandidateDto> candidates = createActionCandidates(userId, currentState.emotion());
//
//        return new LlmRecommendationRequestDto(
//                currentState,preference,candidates);
//    }

    private RecommendationLocationDto resolveLocation(
            Long userId,
            RecommendationGenerateRequest request
    ) {

        if (request.locationMode() == null) {
            throw new IllegalArgumentException(
                    "위치 사용 방식을 선택해주세요."
            );
        }

        return switch (request.locationMode()) {

            case NONE ->
                    new RecommendationLocationDto(
                            LocationMode.NONE,
                            null,
                            null,
                            null
                    );

            case CURRENT -> {

                if (request.latitude() == null
                        || request.longitude() == null) {

                    throw new IllegalArgumentException(
                            "현재 위치 좌표가 필요합니다."
                    );
                }

                yield new RecommendationLocationDto(
                        LocationMode.CURRENT,
                        null,
                        request.latitude(),
                        request.longitude()
                );
            }

            case SAVED -> {

                UserPreference preference =
                        userPreferenceRepository
                                .findByUser_Id(userId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "저장된 기본 위치가 없습니다."
                                        )
                                );

                if (preference.getDefaultRegionLatitude() == null
                        || preference.getDefaultRegionLongitude() == null) {

                    throw new IllegalArgumentException(
                            "저장된 기본 위치가 없습니다."
                    );
                }

                yield new RecommendationLocationDto(
                        LocationMode.SAVED,
                        preference.getDefaultRegionName(),
                        preference.getDefaultRegionLatitude(),
                        preference.getDefaultRegionLongitude()
                );
            }
        };
    }

    public LlmRecommendationResult generateRecommendations(
            Long userId,
            Long moodEntryId,
            RecommendationGenerateRequest request
    ) {

        // 1. LLM에 넘길 데이터 생성
        LlmRecommendationRequestDto llmRequest =
                createLlmRequest(
                        userId,
                        moodEntryId,
                        request
                );

        // 2. Gemini에게 추천 요청
        LlmRecommendationResult result =
                recommendationLlmGenerator.generate(
                        llmRequest
                );

        // 3. 실제 DB 행동 후보에 존재하는 actionCode만 허용
        List<String> validActionCodes =
                llmRequest.candidates()
                        .stream()
                        .map(ActionCandidateDto::actionCode)
                        .toList();


        List<LlmRecommendedActionDto> validated =
                result.recommendations()
                        .stream()
                        .filter(recommendation ->
                                validActionCodes.contains(
                                        recommendation.actionCode()
                                )
                        )
                        .limit(3)
                        .toList();


        if (validated.isEmpty()) {
            throw new IllegalStateException(
                    "추천 가능한 행동이 없습니다."
            );
        }


        return new LlmRecommendationResult(
                validated
        );
    }


}
