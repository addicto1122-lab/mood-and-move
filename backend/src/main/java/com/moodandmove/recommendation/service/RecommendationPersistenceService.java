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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationPersistenceService {

    private final MoodEntryRepository moodEntryRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationRepository recommendationRepository;
    private final ActionRepository actionRepository;
    private final UserActionStatService userActionStatService;

    // 이미 저장된 추천 조회
    @Transactional(readOnly = true)
    public Optional<RecommendationResponse> findExisting(
            Long userId,
            Long moodEntryId
    ) {
        // 본인 소유이고 삭제되지 않은 일기인지 먼저 확인
        moodEntryRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(moodEntryId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "감정 기록을 찾을 수 없습니다."
                ));

        return sessionRepository.findByMoodEntry_Id(moodEntryId)
                .map(this::toResponse);
    }

    // 추천 세션, 추천 행동, 통계를 함께 저장
    @Transactional
    public RecommendationResponse save(
            Long userId,
            Long moodEntryId,
            TimeBucket timeBucket,
            RecommendationType recommendationType,
            List<LlmRecommendedActionDto> selected
    ) {
        // 동일 일기에 대한 동시 저장을 막기 위해 잠금
        var moodEntry = moodEntryRepository
                .findOwnedForUpdate(moodEntryId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "감정 기록을 찾을 수 없습니다."
                ));

        // 다른 요청이 먼저 저장했을 수 있으므로 다시 확인
        var existing = sessionRepository.findByMoodEntry_Id(moodEntryId);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        if (recommendationType == null
                || selected == null
                || selected.isEmpty()
                || selected.size() > 3) {
            throw new IllegalArgumentException(
                    "저장할 추천 정보가 올바르지 않습니다."
            );
        }

        Map<String, Action> actionsByCode =
                actionRepository.findAllByActiveTrue().stream()
                        .collect(Collectors.toMap(
                                Action::getActionCode,
                                Function.identity()
                        ));

        // 저장 직전에도 행동과 추천 이유를 확인
        for (var item : selected) {
            if (item == null
                    || item.actionCode() == null
                    || !actionsByCode.containsKey(item.actionCode())
                    || item.reason() == null
                    || item.reason().isBlank()) {
                throw new IllegalArgumentException(
                        "저장할 추천 후보가 유효하지 않습니다."
                );
            }
        }

        long uniqueCount = selected.stream()
                .map(LlmRecommendedActionDto::actionCode)
                .distinct()
                .count();

        if (uniqueCount != selected.size()) {
            throw new IllegalArgumentException(
                    "같은 행동을 중복 저장할 수 없습니다."
            );
        }

        // 추천 세션 저장
        var session = sessionRepository.save(
                RecommendationSession.create(
                        moodEntry,
                        timeBucket,
                        recommendationType
                )
        );

        List<RecommendationResponse.Item> items = new ArrayList<>();

        // 추천 순서대로 개별 행동 저장
        for (var item : selected) {
            Action action = actionsByCode.get(item.actionCode());

            Recommendation recommendation = Recommendation.create(
                    session,
                    action,
                    items.size() + 1,
                    item.reason().strip()
            );

            Recommendation saved =
                    recommendationRepository.save(recommendation);

            items.add(RecommendationResponse.Item.from(saved));

            // 추천 횟수는 원래 일기 날짜가 속한 월에 반영
            userActionStatService.recordRecommendation(
                    userId,
                    action.getId(),
                    moodEntry.getEmotion().getEmotionCode(),
                    moodEntry.getEntryDate()
            );
        }

        // 일기의 추천 상태 변경
        moodEntry.markRecommendationRequested();

        return new RecommendationResponse(session.getId(), items);
    }

    // 저장된 추천을 화면용 응답으로 변환
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