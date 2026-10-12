package com.moodandmove.recommendation.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "recommendation_generation_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationGenerationLog {

    // MongoDB 문서 ID
    @Id
    private String id;

    // 추천 생성 요청 식별값
    private String requestId;

    // MySQL 추천 세션 ID: 저장 전이나 실패 시에는 null
    private Long sessionId;

    // COLD_START / HYBRID / PERSONALIZED
    private String recommendationType;

    // 실제 호출한 모델
    private String model;

    // 사용한 프롬프트 버전
    private String promptVersion;

    // 추천 당시 감정, 시간대, 선호, 카테고리 점수 등
    private Map<String, Object> input;

    // 파싱된 LLM 응답
    private Map<String, Object> output;

    // JSON 파싱 실패 시에도 응답을 확인하기 위한 원문
    private String rawResponse;

    // SUCCESS / FAILED
    private String status;

    // 실패한 단계와 원인
    private String errorStage;
    private String errorMessage;

    // LLM 호출부터 응답 수신까지 걸린 시간(ms)
    private Long latencyMs;

    // 요청 시작 시각
    private Instant createdAt;
}