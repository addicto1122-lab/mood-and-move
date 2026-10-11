package com.moodandmove.report.llm;

import com.moodandmove.report.dto.MonthlyReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Component
public class MonthlyReportEncouragementGenerator {

    private final RestClient restClient;
    private final String model;

    public MonthlyReportEncouragementGenerator(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.model = model;

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .build();
    }

    public String generate(
            String nickname,
            MonthlyReport report
    ) {

        String prompt = """
                당신은 Mood & Move의 월간 리포트 도우미입니다.

                아래 사용자의 한 달 기록을 바탕으로
                따뜻하고 부담스럽지 않은 응원의 한마디를 작성하세요.

                규칙:
                - 한국어로 작성하세요.
                - 1~2문장만 작성하세요.
                - 최대 100자 정도로 작성하세요.
                - 사용자를 평가하거나 훈계하지 마세요.
                - 정신건강 상태를 진단하거나 단정하지 마세요.
                - 기록이 적어도 부정적으로 표현하지 마세요.
                - 작은 기록이나 행동도 자연스럽게 격려하세요.
                - 과장된 표현이나 효과 보장은 하지 마세요.
                - 따옴표, 마크다운, 제목 없이 문장만 출력하세요.

                사용자 닉네임: %s
                대상 월: %d년 %d월
                일기 작성일 수: %d일
                평균 기분 점수: %s점
                가장 많이 기록한 감정: %s
                행동 실행 횟수: %d회
                평균 기분 변화: %s점
                가장 효과가 좋았던 행동: %s
                """.formatted(
                nickname,
                report.year(),
                report.month(),
                report.diaryCount(),
                report.averageMoodScore(),
                report.topEmotionName() == null
                        ? "기록 없음"
                        : report.topEmotionName(),
                report.executionCount(),
                report.averageDelta(),
                report.bestActionName() == null
                        ? "없음"
                        : report.bestActionName()
        );

        GeminiRequest request = new GeminiRequest(
                List.of(
                        new Content(
                                List.of(
                                        new Part(prompt)
                                )
                        )
                )
        );

        JsonNode response = restClient.post()
                .uri(
                        "/v1beta/models/"
                                + model
                                + ":generateContent"
                )
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        String result = response
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();

        if (result == null || result.isBlank()) {
            throw new IllegalStateException(
                    "월간 응원 문구 생성에 실패했습니다."
            );
        }

        return result.strip();
    }

    private record GeminiRequest(
            List<Content> contents
    ) {
    }

    private record Content(
            List<Part> parts
    ) {
    }

    private record Part(
            String text
    ) {
    }
}