package com.moodandmove.recommendation.service;


import com.moodandmove.recommendation.domain.document.RecommendationGenerationLog;
import com.moodandmove.recommendation.repository.RecommendationGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationGenerationLogService {

    private final RecommendationGenerationLogRepository logRepository;
    private final MongoTemplate mongoTemplate;

    public void save(RecommendationGenerationLog generationLog) {
        try {
            logRepository.save(generationLog);
        } catch (Exception e) {
            // 로그 저장 실패가 추천 처리를 중단시키지 않도록 처리
            log.warn(
                    "추천 생성 로그 저장 실패: requestId={}, errorType={}",
                    generationLog.getRequestId(),
                    e.getClass().getSimpleName()
            );
        }
    }

    // MySQL 저장 성공 후 세션 ID와 최종 상태 반영
    public void markSuccess(String requestId, Long sessionId) {
        Update update = new Update()
                .set("sessionId",sessionId)
                .set("status", "SUCCESS")
                .unset("errorStage")
                .unset("errorMessage");

        updateLog(requestId,update);
    }

    // 추천 검증 또는 MySQL 저장 실패 기록
    public void markFailure(String requestId, String errorStage, Exception exception) {
        Update update = new Update()
                .set("status","FAILED")
                .set("errorStage",errorStage)
                .set("errorMessage",exception.getClass().getSimpleName());

        updateLog(requestId,update);
    }

    // 해당 요청의 LLM_SUCCESS 로그만 갱신
    private void updateLog(String requestId, Update update) {
        if (requestId == null || requestId.isBlank()) {
            log.warn("추천 로그 갱신 생략: requestId가 없습니다.");
            return;
        }
        try {
            Query query = Query.query(
                    Criteria.where("requestId").is(requestId)
                            .and("status").is("LLM_SUCCESS")
            );

            var result = mongoTemplate.updateFirst(
                    query,update,RecommendationGenerationLog.class
            );

            if (result.getMatchedCount() == 0) {
                log.warn(
                        "갱신할 추천 로그 없음 또는 이미 처리됨: requestId={}",
                        requestId
                );
            }
        } catch (Exception e) {
            log.warn(
                    "추천 로그 갱신 실패: requestId={}, errorType={}",
                    requestId,
                    e.getClass().getSimpleName()
            );
        }
    }
}