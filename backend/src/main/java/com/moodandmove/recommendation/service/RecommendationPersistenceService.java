
package com.moodandmove.recommendation.service;

import com.moodandmove.analysis.service.UserActionStatService;
import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.recommendation.domain.dto.LlmRecommendedActionDto;
import com.moodandmove.recommendation.domain.dto.response.RecommendationResponse;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.recommendation.domain.entity.Recommendation;
import com.moodandmove.recommendation.domain.entity.RecommendationSession;
import com.moodandmove.recommendation.domain.type.RecommendationType;
import com.moodandmove.recommendation.repository.ActionRepository;
import com.moodandmove.recommendation.repository.RecommendationRepository;
import com.moodandmove.recommendation.repository.RecommendationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationPersistenceService {

    private final MoodEntryRepository moodEntryRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationRepository recommendationRepository;
    private final ActionRepository actionRepository;
    private final UserActionStatService userActionStatService;

    private static final Set<String> ACTION_CATEGORIES = Set.of(
            "WALK", "EXERCISE", "STRETCHING",
            "MEDITATION", "SLEEP", "MUSIC",
            "READING", "ENTERTAINMENT", "SOCIAL",
            "OUTDOOR", "EATING", "SELF_CARE", "CLEANING"
    );

    // 이미 저장된 추천 조회
    @Transactional(readOnly = true)
    public Optional<RecommendationResponse> findExisting(
            Long userId,
            Long moodEntryId
    ) {
        moodEntryRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        moodEntryId, userId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "감정 기록을 찾을 수 없습니다."
                ));

        return sessionRepository.findByMoodEntry_Id(moodEntryId)
                .map(this::toResponse);
    }

    // 추천 세션 + 행동 3개 + 추천 3개 저장
    @Transactional
    public RecommendationResponse save(
            Long userId,
            Long moodEntryId,
            TimeBucket timeBucket,
            RecommendationType recommendationType,
            List<LlmRecommendedActionDto> selected
    ) {
        // 동일 일기에서 중복 추천 세션 생성 방지
        var moodEntry = moodEntryRepository
                .findOwnedForUpdate(moodEntryId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "감정 기록을 찾을 수 없습니다."
                ));

        // 이미 저장된 추천이 있다면 기존 결과 반환
        var existing = sessionRepository.findByMoodEntry_Id(moodEntryId);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        if (timeBucket == null
                || recommendationType == null
                || selected == null
                || selected.size() != 3) {
            throw new IllegalArgumentException(
                    "추천 정보는 정확히 3개여야 합니다."
            );
        }

        // LLM 추천 결과 사전 검증
        Set<String> uniqueNames = new HashSet<>();

        for (LlmRecommendedActionDto item : selected) {
            validateAction(item);

            String actionName = normalizeName(item.actionName());
            String normalizedKey = actionName.toLowerCase(Locale.ROOT);

            if (!uniqueNames.add(normalizedKey)) {
                throw new IllegalArgumentException(
                        "같은 행동을 중복 추천할 수 없습니다."
                );
            }
        }

        // 추천 세션 생성
        RecommendationSession session = sessionRepository.save(
                RecommendationSession.create(
                        userId,
                        moodEntry,
                        timeBucket,
                        recommendationType
                )
        );

        List<RecommendationResponse.Item> items = new ArrayList<>();

        for (int i = 0; i < selected.size(); i++) {
            LlmRecommendedActionDto item = selected.get(i);

            // 이름으로 Action 재사용 또는 신규 생성
            Action action = findOrCreateAction(item);

            // 순위는 1, 2, 3
            Recommendation recommendation = Recommendation.create(
                    session,
                    action,
                    i + 1,
                    item.reason().strip()
            );

            Recommendation saved =
                    recommendationRepository.save(recommendation);

            items.add(RecommendationResponse.Item.from(saved));

            // 기존 행동별 추천 횟수 통계 기록
            userActionStatService.recordRecommendation(
                    userId,
                    action.getId(),
                    moodEntry.getEmotion().getEmotionCode(),
                    moodEntry.getEntryDate()
            );
        }

        // 일기의 추천 요청 상태 변경
        moodEntry.markRecommendationRequested();

        return new RecommendationResponse(session.getId(), items);
    }

    // 같은 이름의 행동은 기존 데이터를 재사용
    private Action findOrCreateAction(LlmRecommendedActionDto item) {
        String actionName = normalizeName(item.actionName());

        return actionRepository.findByActionName(actionName)
                .orElseGet(() -> actionRepository.save(
                        Action.create(
                                actionName,
                                item.category(),
                                item.durationMinutes(),
                                item.environmentType(),
                                item.socialType(),
                                item.activityStyle(),
                                item.locationRequired(),
                                item.placeCategory()
                        )
                ));
    }

    // LLM 추천 행동 검증
    private void validateAction(LlmRecommendedActionDto item) {
        if (item == null
                || item.actionName() == null
                || item.actionName().isBlank()
                || item.reason() == null
                || item.reason().isBlank()
                || item.category() == null
                || !ACTION_CATEGORIES.contains(item.category())
                || item.durationMinutes() == null
                || item.durationMinutes() <= 0
                || item.environmentType() == null
                || item.socialType() == null
                || item.activityStyle() == null) {
            throw new IllegalArgumentException(
                    "LLM 추천 행동 정보가 올바르지 않습니다."
            );
        }

        if (normalizeName(item.actionName()).length() > 100) {
            throw new IllegalArgumentException(
                    "행동 이름은 100자를 초과할 수 없습니다."
            );
        }

        if (item.placeCategory() != null
                && item.placeCategory().length() > 50) {
            throw new IllegalArgumentException(
                    "장소 카테고리는 50자를 초과할 수 없습니다."
            );
        }
    }

    // 행동 이름의 앞뒤 공백 및 연속 공백 정리
    private String normalizeName(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }

    // 저장된 추천을 응답 DTO로 변환
    private RecommendationResponse toResponse(
            RecommendationSession session
    ) {
        List<RecommendationResponse.Item> items =
                recommendationRepository
                        .findAllBySession_IdOrderByRankNoAsc(session.getId())
                        .stream()
                        .map(RecommendationResponse.Item::from)
                        .toList();

        return new RecommendationResponse(session.getId(), items);
    }
}
