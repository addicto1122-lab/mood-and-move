package com.moodandmove.mood.service;

import com.moodandmove.mood.dto.request.MoodCreateWithRecommendationRequest;
import com.moodandmove.mood.dto.response.MoodCreateWithRecommendationResponse;
import com.moodandmove.recommendation.service.RecommendationGenerationLogService;
import com.moodandmove.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class MoodRecommendationService {

    private final RecommendationService recommendationService;
    private final MoodRecommendationPersistenceService persistenceService;
    private final RecommendationGenerationLogService generationLogService;
    private final MoodService moodService;

    // LLM 응답을 기다리는 동안 DB 트랜잭션을 열지 않음
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MoodCreateWithRecommendationResponse create(
            Long userId,
            MoodCreateWithRecommendationRequest request
    ) {
        LocalDateTime recordedAt =
                LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 저장 가능한 상태인지 먼저 확인
        moodService.validateCanCreate(userId, recordedAt.toLocalDate());

        // 1. 입력한 일기로 추천 생성·검증
        // 실패하면 아직 MySQL에 일기를 저장하지 않은 상태
        var prepared = recommendationService.prepareRecommendation(
                userId,
                request.mood(),
                request.recommendation(),
                recordedAt
        );

        MoodCreateWithRecommendationResponse response;

        try {
            // 2. 별도 서비스의 트랜잭션에서 일기 + 추천 저장
            // 이 호출이 정상 반환되면 커밋까지 완료된 상태
            response = persistenceService.save(
                    userId,
                    request.mood(),
                    prepared,
                    recordedAt
            );
        } catch (RuntimeException e) {
            generationLogService.markFailure(
                    prepared.requestId(),
                    "MYSQL_SAVE",
                    e
            );

            throw e;
        }

        // 3. MySQL 커밋 완료 후 MongoDB 로그에 성공 반영
        generationLogService.markSuccess(
                prepared.requestId(),
                response.recommendation().sessionId()
        );

        return response;
    }
}