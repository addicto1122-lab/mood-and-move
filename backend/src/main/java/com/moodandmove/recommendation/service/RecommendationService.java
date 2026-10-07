package com.moodandmove.recommendation.service;

import com.moodandmove.analysis.dto.ActionPersonalStatResponse;
import com.moodandmove.analysis.service.UserActionStatService;
import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.recommendation.domain.dto.*;
import com.moodandmove.recommendation.domain.dto.response.RecommendationResponse;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.recommendation.domain.type.RecommendationType;
import com.moodandmove.recommendation.llm.RecommendationLlmGenerator;
import com.moodandmove.recommendation.repository.ActionRepository;
import com.moodandmove.user.domain.entity.UserPreference;
import com.moodandmove.user.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.moodandmove.recommendation.domain.dto.*;

import com.moodandmove.place.domain.type.LocationMode;

import com.moodandmove.recommendation.domain.dto.RecommendationGenerateRequest;
import com.moodandmove.recommendation.domain.dto.RecommendationLocationDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final ActionRepository actionRepository;

    private final UserPreferenceRepository userPreferenceRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final TimeBucketResolver timeBucketResolver;

    private final UserActionStatService userActionStatService;
    private final PersonalScoreCalculator personalScoreCalculator;
    private final RecommendationLlmGenerator recommendationLlmGenerator;

    private final RecommendationPersistenceService persistenceService;

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
                            personalScore,
                            emotionStat.sampleCount()
                    )
            );
        }

        return candidates;
    }

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

        // 5. 사용자 전체 재측정 횟수로 추천 유형 결정
        long totalSampleCount =
                userActionStatService.getTotalSampleCount(userId);

        RecommendationType recommendationType =
                resolveRecommendationType(totalSampleCount);

    // 6. 추천 유형을 포함한 LLM 요청 데이터 생성
        return new LlmRecommendationRequestDto(
                currentState,
                location,
                preference,
                candidates,
                recommendationType
        );
    }


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

    public RecommendationResponse generateRecommendations(
            Long userId,
            Long moodEntryId,
            RecommendationGenerateRequest request
    ) {
        // 이미 저장된 추천이 있으면 그대로 반환
        var existing = persistenceService.findExisting(userId, moodEntryId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 현재 상태, 위치, 선호, 행동 후보를 준비
        var llmRequest = createLlmRequest(userId, moodEntryId, request);

        if (llmRequest.candidates().isEmpty()) {
            throw new IllegalStateException("추천 가능한 행동이 없습니다.");
        }

        // Gemini 호출 후 응답 검증
        var result = recommendationLlmGenerator.generate(llmRequest);

        var validated = validateRecommendations(
                result,
                llmRequest.candidates()
        );

        // 추천을 DB에 저장하고 화면용 응답 반환

        RecommendationType recommendationType = llmRequest.recommendationType();

    // 추천을 DB에 저장하고 화면용 응답 반환
        return persistenceService.save(
                userId,
                moodEntryId,
                llmRequest.currentState().timeBucket(),
                recommendationType,
                validated
        );
    }
    private List<LlmRecommendedActionDto> validateRecommendations(
            LlmRecommendationResult result,
            List<ActionCandidateDto> candidates
    ) {
        if (result == null || result.recommendations() == null) {
            throw new IllegalStateException("추천 응답이 없습니다.");
        }

        List<String> validCodes = candidates.stream()
                .map(ActionCandidateDto::actionCode)
                .toList();

        List<LlmRecommendedActionDto> validated = new ArrayList<>();
        List<String> selectedCodes = new ArrayList<>();

        for (LlmRecommendedActionDto item : result.recommendations()) {
            if (item == null
                    || !validCodes.contains(item.actionCode())
                    || selectedCodes.contains(item.actionCode())
                    || item.reason() == null
                    || item.reason().isBlank()) {
                continue;
            }

            validated.add(new LlmRecommendedActionDto(
                    item.actionCode(),
                    item.reason().strip()
            ));

            selectedCodes.add(item.actionCode());

            if (validated.size() == 3) {
                break;
            }
        }

        if (validated.isEmpty()) {
            throw new IllegalStateException("추천 가능한 행동이 없습니다.");
        }

        return validated;
    }

    static RecommendationType resolveRecommendationType(long totalSampleCount) {
        if (totalSampleCount <= 3) {
            return RecommendationType.COLD_START;
        }

        if (totalSampleCount <= 10) {
            return RecommendationType.HYBRID;
        }

        return RecommendationType.PERSONALIZED;
    }
}
