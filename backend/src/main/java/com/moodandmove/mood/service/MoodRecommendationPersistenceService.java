package com.moodandmove.mood.service;

import com.moodandmove.mood.dto.request.MoodCreateRequest;
import com.moodandmove.mood.dto.response.MoodCreateWithRecommendationResponse;
import com.moodandmove.recommendation.domain.dto.PreparedRecommendation;
import com.moodandmove.recommendation.service.RecommendationPersistenceService;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MoodRecommendationPersistenceService {

    private final MoodService moodService;
    private final RecommendationPersistenceService recommendationPersistenceService;
    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    public MoodCreateWithRecommendationResponse save(
            Long userId,
            MoodCreateRequest moodRequest,
            PreparedRecommendation prepared,
            LocalDateTime recordedAt
    ) {
        var llmRequest = prepared.request();

        if (!userId.equals(llmRequest.userId())) {
            throw new IllegalArgumentException(
                    "추천 요청의 사용자가 일치하지 않습니다."
            );
        }

        // 같은 사용자의 동시 저장 요청을 순서대로 처리
        userRepository.findForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다."
                ));

        // 기존 날짜 중복·미완료 재측정 검사 후 일기 저장
        Long moodEntryId = moodService.createMood(
                userId,
                moodRequest,
                recordedAt
        );

        // 검증이 끝난 추천을 같은 트랜잭션에서 저장
        var recommendation = recommendationPersistenceService.save(
                userId,
                moodEntryId,
                llmRequest.currentState().timeBucket(),
                llmRequest.recommendationType(),
                llmRequest.location().locationMode(),
                llmRequest.location().latitude(),
                llmRequest.location().longitude(),
                prepared.actions()
        );

        return new MoodCreateWithRecommendationResponse(
                moodEntryId,
                recommendation
        );
    }
}