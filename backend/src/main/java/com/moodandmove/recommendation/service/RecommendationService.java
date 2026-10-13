
package com.moodandmove.recommendation.service;

import com.moodandmove.analysis.dto.ActionPersonalStatResponse;
import com.moodandmove.analysis.service.UserActionStatService;
import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;
import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.place.domain.type.LocationMode;
import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.recommendation.domain.dto.*;
import com.moodandmove.recommendation.domain.dto.response.RecommendationResponse;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.recommendation.domain.type.ActionCategory;
import com.moodandmove.recommendation.domain.type.RecommendationType;
import com.moodandmove.recommendation.llm.RecommendationLlmGenerator;
import com.moodandmove.recommendation.repository.ActionRepository;
import com.moodandmove.user.domain.entity.UserPreference;
import com.moodandmove.user.repository.UserHobbyRepository;
import com.moodandmove.user.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
    private final CategoryScoreService categoryScoreService;

    private final RecommendationGenerationLogService generationLogService;
    private final UserHobbyRepository userHobbyRepository;

    private static final Set<String> ACTION_CATEGORIES =
            Arrays.stream(ActionCategory.values())
                    .map(Enum::name)
                    .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> PLACE_CATEGORIES =
            Arrays.stream(PlaceType.values())
                    .map(Enum::name)
                    .collect(Collectors.toUnmodifiableSet());

    // 개인화 점수 계산
    public BigDecimal calculatePersonalScore(
            ActionPersonalStatResponse emotionStat
    ) {
        return personalScoreCalculator.calculate(
                emotionStat.avgDelta(),
                emotionStat.positiveCount(),
                emotionStat.sampleCount()
        );
    }

    // 기존 행동들의 개인화 점수를 LLM 참고자료로 구성
    public List<ActionCandidateDto> createActionCandidates(
            Long userId,
            String emotionCode,
            LocationMode locationMode
    ) {
        // V9에서 active 컬럼 삭제
        List<Action> actions = actionRepository.findAll();

        List<ActionCandidateDto> candidates = new ArrayList<>();

        for (Action action : actions) {

            // 위치를 사용하지 않을 경우 장소 필수 행동 제외
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
                    calculatePersonalScore(emotionStat);

            candidates.add(
                    new ActionCandidateDto(
                            action.getId(),
                            action.getActionName(),
                            personalScore,
                            emotionStat.sampleCount()
                    )
            );
        }

        return candidates;
    }

    // 감정 일기로부터 현재 상태 조회
    private CurrentStateDto createCurrentState(
            Long userId,
            Long moodEntryId
    ) {
        MoodEntry moodEntry = moodEntryRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        moodEntryId,
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
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
                moodEntry.getCurrentActivity(),
                moodEntry.getDiaryContent(),
                timeBucket
        );
    }

    // 사용자 선호 조회
    private UserPreferenceDto createUserPreference(Long userId) {

        List<String> hobbies = userHobbyRepository.findHobbyNamesByUserId(userId);

        return userPreferenceRepository
                .findByUser_Id(userId)
                .map(preference ->
                        new UserPreferenceDto(
                                preference.getActivityEnvironment(),
                                preference.getActivityStyle(),
                                preference.getSocialPreference(),
                                preference.getDefaultAvailableMinutes(),
                                hobbies
                        )
                )
                .orElseGet(() -> new UserPreferenceDto(
                                EnvironmentType.ANY,
                                ActivityStyle.ANY,
                                SocialType.ANY,
                                null,
                                hobbies
                ));
    }

    // Gemini에 전달할 데이터 생성
    public LlmRecommendationRequestDto createLlmRequest(
            Long userId,
            Long moodEntryId,
            RecommendationGenerateRequest request
    ) {
        // 1. 감정 일기 기반 현재 상태
        CurrentStateDto currentState =
                createCurrentState(userId, moodEntryId);

        // 2. 사용자 위치
        RecommendationLocationDto location =
                resolveLocation(userId, request);

        // 3. 온보딩 선호
        UserPreferenceDto preference =
                createUserPreference(userId);

        // 4. DB에 저장된 사용자·카테고리별 점수와 표본 수 조회
        List<CategoryScoreDto> categoryScores =
                categoryScoreService.getScores(userId);

        // 카테고리 기본 데이터가 없으면 설정 오류로 처리
        if (categoryScores.isEmpty()) {
            throw new IllegalStateException(
                    "카테고리 기본 데이터가 없습니다. V11 적용을 확인해주세요."
            );
        }

        // 5. 모든 카테고리의 재측정 완료 표본 합산
        long totalSampleCount = categoryScores.stream()
                .mapToLong(CategoryScoreDto::sampleCount)
                .sum();

        // 6. 기존 분류 기준으로 추천 유형 결정
        RecommendationType recommendationType =
                resolveRecommendationType(totalSampleCount);

        // 7. 행동 후보 대신 카테고리 통계를 전달
        return new LlmRecommendationRequestDto(
                currentState,
                location,
                preference,
                categoryScores,
                recommendationType
        );
    }

    // 위치 사용 방식 결정
    private RecommendationLocationDto resolveLocation(
            Long userId,
            RecommendationGenerateRequest request
    ) {
        if (request == null || request.locationMode() == null) {
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

    // 실제 추천 생성
    public RecommendationResponse generateRecommendations(
            Long userId,
            Long moodEntryId,
            RecommendationGenerateRequest request
    ) {
        // 1. 기존 추천 조회
        var existing =
                persistenceService.findExisting(userId, moodEntryId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. LLM 요청 데이터 생성
        var llmRequest =
                createLlmRequest(userId, moodEntryId, request);

        // 기존 행동 후보가 없어도 Gemini 호출 가능
        // Cold Start에서는 신규 행동을 생성할 수 있어야 함

        // 3. Gemini 호출
        LlmRecommendationResult result =
                recommendationLlmGenerator.generate(llmRequest);

        String requestId = result.requestId();
        String stage = "VALIDATE_RECOMMENDATIONS";

        RecommendationResponse response;

        try {
            // 4. 생성 결과 검증
            List<LlmRecommendedActionDto> validated =
                    validateRecommendations(
                            result,
                            llmRequest.location().locationMode()
                    );

            // 5. 추천 및 행동 저장

            stage = "MYSQL_SAVE";

            response = persistenceService.save(
                    userId,
                    moodEntryId,
                    llmRequest.currentState().timeBucket(),
                    llmRequest.recommendationType(),
                    llmRequest.location().locationMode(),
                    validated
            );
        }catch (RuntimeException e) {
            //검증 실패 또는 MySQL 저장 실패를 로그에 반영
            generationLogService.markFailure(requestId,stage,e);

            throw e;
        }

        // 6. MySQL 저장 완료 후 MongoDB 로그 연결
        generationLogService.markSuccess(
                requestId,response.sessionId());
        return response;
    }

    // LLM 추천 결과 검증
    private List<LlmRecommendedActionDto> validateRecommendations(
            LlmRecommendationResult result,
            LocationMode locationMode
    ) {
        if (result == null || result.recommendations() == null) {
            throw new IllegalStateException(
                    "추천 응답이 없습니다."
            );
        }

        List<LlmRecommendedActionDto> validated =
                new ArrayList<>();

        Set<String> selectedNames = new HashSet<>();

        for (LlmRecommendedActionDto item : result.recommendations()) {

            if (item == null
                    || item.actionName() == null
                    || item.actionName().isBlank()
                    || item.reason() == null
                    || item.reason().isBlank()
                    || item.category() == null
                    || !ACTION_CATEGORIES.contains(item.category())
                    || !item.hasValidDuration()
                    || item.environmentType() == null
                    || item.socialType() == null
                    || item.activityStyle() == null) {
                continue;
            }
            // 여행은 지역 수준으로 추천하며 상세 장소 검색은 사용하지 않음
            if ("TRAVEL".equals(item.category())
                    && (item.locationRequired() || item.placeCategory() != null)) {
                continue;
            }

            String actionName =
                    normalizeName(item.actionName());

            if (actionName.length() > 100
                    || item.placeCategory() != null
                    && item.placeCategory().length() > 50) {
                continue;
            }

            String normalizedPlaceCategory = null;

            if (item.locationRequired()) {

                if (item.placeCategory() == null
                        || item.placeCategory().isBlank()) {
                    continue;
                }

                normalizedPlaceCategory =
                        item.placeCategory()
                                .strip()
                                .toUpperCase(Locale.ROOT);

                if (!PLACE_CATEGORIES.contains(normalizedPlaceCategory)) {
                    continue;
                }
            }

            // 위치를 사용하지 않는다면 장소 필수 행동 제외
            if (locationMode == LocationMode.NONE
                    && item.locationRequired()) {
                continue;
            }

            String normalizedKey =
                    actionName.toLowerCase(Locale.ROOT);

            if (!selectedNames.add(normalizedKey)) {
                continue;
            }

            validated.add(
                    new LlmRecommendedActionDto(
                            actionName,
                            item.category(),
                            item.durationMinutes(),
                            item.durationDays(),
                            item.durationNights(),
                            item.environmentType(),
                            item.socialType(),
                            item.activityStyle(),
                            item.locationRequired(),
                            normalizedPlaceCategory,
                            item.reason().strip(),
                            item.emoji()
                    )
            );

            if (validated.size() == 3) {
                break;
            }
        }

        // 최종 3개 미만이면 저장하지 않음
        if (validated.size() != 3) {
            throw new IllegalStateException(
                    "유효한 추천 행동 3개를 생성하지 못했습니다."
            );
        }

        return validated;
    }

    // 행동 이름 정규화
    private String normalizeName(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }

    // 사용자 전체 유효 재측정 표본 수에 따른 추천 유형
    static RecommendationType resolveRecommendationType(
            long totalSampleCount
    ) {
        if (totalSampleCount < 3) {
            return RecommendationType.COLD_START;
        }

        if (totalSampleCount < 10) {
            return RecommendationType.HYBRID;
        }

        return RecommendationType.PERSONALIZED;
    }
}
