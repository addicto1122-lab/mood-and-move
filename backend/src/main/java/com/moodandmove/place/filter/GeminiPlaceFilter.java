package com.moodandmove.place.filter;


import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceFilterResult;
import com.moodandmove.place.dto.PlaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

@Component
@Primary
public class GeminiPlaceFilter implements PlaceFilter {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;


    public GeminiPlaceFilter(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {

        this.objectMapper = objectMapper;
        this.model = model;

        System.out.println(
                "Gemini API Key loaded = "
                        + (apiKey != null && !apiKey.isBlank())
        );

        this.restClient = RestClient.builder()
                .baseUrl(
                        "https://generativelanguage.googleapis.com"
                )
                .defaultHeader(
                        "x-goog-api-key",
                        apiKey
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .build();
    }


    @Override
    public List<PlaceResponse> filter(
            PlaceType placeType,
            List<PlaceResponse> candidates
    ) {

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }


        /*
         * =========================
         * Gemini 전달 전 후보
         * =========================
         */
        System.out.println();
        System.out.println(
                "===== Gemini 전달 전 후보 ====="
        );

        candidates.forEach(place ->
                System.out.println(
                        place.placeId()
                                + " / "
                                + place.name()
                                + " / "
                                + place.category()
                                + " / "
                                + place.distance()
                                + "m"
                )
        );


        String prompt =
                buildPrompt(
                        placeType,
                        candidates
                );


        try {

            /*
             * =========================
             * Gemini API 호출
             * =========================
             */
            GeminiRequest request =
                    new GeminiRequest(
                            List.of(
                                    new Content(
                                            List.of(
                                                    new Part(
                                                            prompt
                                                    )
                                            )
                                    )
                            ),
                            new GenerationConfig(
                                    "application/json"
                            )
                    );


            JsonNode response =
                    restClient.post()
                            .uri(
                                    "/v1beta/models/"
                                            + model
                                            + ":generateContent"
                            )
                            .body(request)
                            .retrieve()
                            .body(JsonNode.class);


            /*
             * Gemini 응답에서
             * 실제 텍스트 추출
             */
            String outputText =
                    extractOutputText(response);


            System.out.println();
            System.out.println(
                    "===== Gemini 원본 응답 ====="
            );

            System.out.println(outputText);


            /*
             * JSON
             * →
             * PlaceFilterResult
             */
            PlaceFilterResult result =
                    objectMapper.readValue(
                            removeMarkdownCodeBlock(
                                    outputText
                            ),
                            PlaceFilterResult.class
                    );


            Set<String> selectedIds =
                    result.selectedPlaceIds() == null
                            ? Set.of()
                            : Set.copyOf(
                            result.selectedPlaceIds()
                    );


            System.out.println();
            System.out.println(
                    "===== Gemini 선택 placeId ====="
            );

            System.out.println(selectedIds);


            /*
             * Gemini가 선택한 ID와
             * Kakao 원본 데이터 매칭
             */
            List<PlaceResponse> filteredPlaces =
                    candidates.stream()
                            .filter(place ->
                                    selectedIds.contains(
                                            place.placeId()
                                    )
                            )
                            .limit(5)
                            .toList();


            System.out.println();
            System.out.println(
                    "===== 최종 장소 결과 ====="
            );

            filteredPlaces.forEach(place ->
                    System.out.println(
                            place.name()
                                    + " / "
                                    + place.category()
                    )
            );


            return filteredPlaces;


        } catch (Exception e) {

            System.err.println(
                    "Gemini 장소 필터링 실패 = "
                            + e.getMessage()
            );

            throw new IllegalStateException(
                    "Gemini 장소 필터링에 실패했습니다.",
                    e
            );
        }
    }


    /*
     * =========================
     * Prompt 생성
     * =========================
     */
    private String buildPrompt(
            PlaceType placeType,
            List<PlaceResponse> candidates
    ) {

        StringBuilder prompt =
                new StringBuilder();


        prompt.append("""
                당신은 Mood & Move 서비스의 장소 후보 필터입니다.

                목표 장소 유형:
                """);

        prompt.append(placeType);


        prompt.append("""

                
                아래 장소 후보 중
                사용자가 실제로 방문하기 적합한 장소만 선택하세요.

                반드시 지켜야 할 규칙:

                1. 반드시 제공된 후보 장소에서만 선택합니다.
                2. 새로운 장소를 생성하지 않습니다.
                3. 최대 5개의 장소만 선택합니다.
                4. 장소 이름에 목표 장소의 단어가 포함되어 있더라도
                   실제 장소 유형이 다르면 제외합니다.
                5. 반드시 제공된 placeId만 반환합니다.

                장소 유형별 판단 기준:

                PARK
                - 실제 산책하거나 휴식할 수 있는 공원을 선택합니다.
                - 음식점, 카페, 상점, 학원 등은 제외합니다.
                - 이름에 '공원'이 있어도 실제 공원이 아니라면 제외합니다.

                CAFE
                - 실제 방문하여 음료를 마시거나
                  휴식할 수 있는 카페를 선택합니다.

                SHOPPING
                - 실제 방문하여 쇼핑하거나 구경할 수 있는 장소입니다.
                - 쇼핑몰, 백화점, 아울렛,
                  오프라인 팝업스토어 등을 포함할 수 있습니다.
                - 인터넷 쇼핑몰, 통신판매업체,
                  사무실 등은 제외합니다.

                LIBRARY
                - 실제 방문하여 책을 읽거나
                  이용할 수 있는 도서관을 선택합니다.

                CINEMA
                - 실제 영화를 관람할 수 있는
                  영화관만 선택합니다.

                후보 장소:
                """);


        for (PlaceResponse place : candidates) {

            prompt.append("\n")
                    .append("- placeId=")
                    .append(place.placeId())

                    .append(", name=")
                    .append(place.name())

                    .append(", category=")
                    .append(place.category())

                    .append(", distance=")
                    .append(place.distance())
                    .append("m");
        }


        prompt.append("""

                
                반드시 아래 JSON 형식으로만 응답하세요.

                {
                  "selectedPlaceIds": [
                    "placeId"
                  ]
                }

                다른 설명은 작성하지 마세요.
                """);


        return prompt.toString();
    }


    /*
     * =========================
     * Gemini 응답 텍스트 추출
     * =========================
     */
    private String extractOutputText(
            JsonNode response
    ) {

        if (response == null) {

            throw new IllegalStateException(
                    "Gemini 응답이 없습니다."
            );
        }


        JsonNode candidates =
                response.path("candidates");


        if (!candidates.isArray()
                || candidates.isEmpty()) {

            throw new IllegalStateException(
                    "Gemini 응답에 candidate가 없습니다."
            );
        }


        JsonNode parts =
                candidates
                        .get(0)
                        .path("content")
                        .path("parts");


        if (!parts.isArray()
                || parts.isEmpty()) {

            throw new IllegalStateException(
                    "Gemini 응답에 content가 없습니다."
            );
        }


        String text =
                parts.get(0)
                        .path("text")
                        .asText();


        if (text == null || text.isBlank()) {

            throw new IllegalStateException(
                    "Gemini 응답 text가 없습니다."
            );
        }


        return text;
    }


    /*
     * 혹시 Markdown 코드블록이
     * 포함됐을 경우 제거
     */
    private String removeMarkdownCodeBlock(
            String text
    ) {

        if (text == null) {
            return "";
        }


        String cleaned =
                text.trim();


        if (cleaned.startsWith("```json")) {

            cleaned =
                    cleaned.substring(7);

        } else if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.substring(3);
        }


        if (cleaned.endsWith("```")) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }


        return cleaned.trim();
    }


    /*
     * =========================
     * Gemini 요청 DTO
     * =========================
     */

    private record GeminiRequest(
            List<Content> contents,
            GenerationConfig generationConfig
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


    private record GenerationConfig(
            String responseMimeType
    ) {
    }
}