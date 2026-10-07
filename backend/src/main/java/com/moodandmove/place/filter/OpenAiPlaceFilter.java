package com.moodandmove.place.filter;

import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceFilterResult;
import com.moodandmove.place.dto.PlaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

//@Component
public class OpenAiPlaceFilter implements PlaceFilter{
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final String model;

    public OpenAiPlaceFilter(
            ObjectMapper objectMapper,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model
    )
    {
        System.out.println(
                "OpenAI API Key loaded = "
                        + (apiKey != null && !apiKey.isBlank())
        );

        this.objectMapper = objectMapper;
        this.model = model;

        /*
         * API Key가 정상적으로 주입됐는지만 확인
         * 실제 Key 값 자체는 출력하지 않음
         */
        System.out.println(
                "OpenAI API Key loaded = "
                        + (apiKey != null && !apiKey.isBlank())
        );

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey
                )
                .build();
    }

    @Override
    public List<PlaceResponse> filter(
            PlaceType placeType,
            List<PlaceResponse> candidates
    )
    {

        /*
         * 후보 장소가 없으면
         * LLM을 호출할 필요가 없음
         */
        if(candidates == null || candidates.isEmpty())
        {
            return List.of();
        }

        /*
         * =========================
         * LLM 전달 전 후보 확인
         * =========================
         */
        System.out.println();
        System.out.println("===== LLM 전달 전 후보 =====");

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

        /*
         * Prompt 생성
         */
        String prompt = buildPrompt(
                placeType,
                candidates
        );

        try{

            /*
             * =========================
             * OpenAI Responses API 호출
             * =========================
             */
            JsonNode response = restClient.post()
                    .uri("/v1/responses")
                    .body(
                            new OpenAiRequest(
                                    model,
                                    prompt
                            )
                    )
                    .retrieve()
                    .body(JsonNode.class);

            /*
             * OpenAI Response에서
             * 실제 모델 출력 텍스트 추출
             */
            String outputText = extractOutputText(response);

//            JsonNode result = objectMapper.readTree(outputText);
//
//            Set<String> selectedIds = objectMapper.convertValue(
//                    result.get("selectedPlaceIds"),
//                    objectMapper.getTypeFactory()
//                            .constructCollectionType(
//                                    Set.class,
//                                    String.class
//                            )
//            );

            /*
             * =========================
             * LLM 원본 응답 확인
             * =========================
             */
            System.out.println();
            System.out.println("===== LLM 원본 응답 =====");
            System.out.println(outputText);

            /*
             * JSON 문자열
             * →
             * PlaceFilterResult DTO
             */
            PlaceFilterResult result = objectMapper.readValue(
                    outputText,
                    PlaceFilterResult.class
            );

            /*
             * selectedPlaceIds
             * 빠른 검색을 위해 Set으로 변환
             */
            Set<String> selectedIds =
                    result.selectedPlaceIds() == null
                            ? Set.of()
                            : Set.copyOf(
                            result.selectedPlaceIds()
                    );
            //Set<String> selectedIds = Set.copyOf(result.selectedPlaceIds());

            /*
             * =========================
             * LLM이 선택한 ID 확인
             * =========================
             */
            System.out.println();
            System.out.println("===== LLM 선택 placeId =====");
            System.out.println(selectedIds);

            /*
             * 중요한 부분
             *
             * LLM이 새로운 장소를 만드는 것이 아니라
             * Kakao에서 받은 원본 후보 중
             * LLM이 선택한 placeId와 일치하는 것만 반환
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


            /*
             * =========================
             * 최종 반환 장소 확인
             * =========================
             */
            System.out.println();
            System.out.println("===== 최종 장소 결과 =====");

            filteredPlaces.forEach(place ->
                    System.out.println(
                            place.name()
                                    + " / "
                                    + place.category()
                    )
            );


            return filteredPlaces;

//            return candidates.stream()
//                    .filter(place -> selectedIds.contains(
//                            place.placeId()
//                        )
//                    )
//                    .limit(5)
//                    .toList();
        }catch(Exception e){
            System.err.println("LLM 장소 필터링 실패 = " + e.getMessage());

            throw new IllegalStateException(
                    "LLM 장소 필터링에 실패했습니다.",
                    e
            );
    }


    }

    /*
     * =========================
     * OpenAI 요청 DTO
     * =========================
     */
    private record OpenAiRequest(
            String model,
            String input
    ){}

    /*
     * =========================
     * LLM Prompt 생성
     * =========================
     */
    private String buildPrompt(
            PlaceType placeType,
            List<PlaceResponse> candidates
    )
    {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                당신은 Mood & Move 서비스의 장소 필터입니다.
                
                목표 장소 유형:
                """);
        prompt.append(placeType);
        prompt.append("""
                아래 장소 후보 중 사용자가 실제로 방문하기 적합한 장소만 선택하세요.
                
                            반드시 지켜야 할 규칙:
                            1. 제공된 후보 장소만 선택합니다.
                            2. 새로운 장소를 생성하지 않습니다.
                            3. 최대 5개의 장소만 선택합니다.
                            4. 장소 이름에 키워드가 포함되어 있어도 실제 장소 유형이 다르면 제외합니다.
                            5. 반드시 placeId만 반환합니다.
                
                            장소 유형별 기준:
                
                            PARK
                            - 실제로 산책하거나 휴식할 수 있는 공원
                            - 음식점, 카페, 상점 등은 제외
                
                            CAFE
                            - 실제 방문하여 음료를 마시거나 휴식할 수 있는 카페
                
                            SHOPPING
                            - 실제 방문하여 쇼핑하거나 구경할 수 있는 장소
                            - 쇼핑몰, 백화점, 아울렛, 팝업스토어 등 포함 가능
                            - 인터넷 쇼핑몰, 통신판매업체 등은 제외
                
                            LIBRARY
                            - 실제 방문 가능한 도서관
                
                            CINEMA
                            - 실제 영화를 관람할 수 있는 영화관
                
                            후보 장소:
                """);

        /*
         * Kakao 후보 장소들을
         * LLM Prompt에 추가
         */
        for(PlaceResponse place : candidates)
        {
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
        /*
         * 응답 형식 제한
         */
        prompt.append("""
                
                
                다음 JSON 형식으로만 응답하세요.
                
                {
                    "selectedPlaceIds": [
                        "placeId"
                    ]
                }
                """);

        return prompt.toString();
    }

    /*
     * =========================
     * Responses API 결과에서
     * output_text 추출
     * =========================
     */
    private String extractOutputText(
            JsonNode response
    )
    {
        if(response == null)
        {
            throw new IllegalStateException(
                    "OpenAi 응답이 없습니다."
            );
        }

        /*
         * Responses API의 output 배열 탐색
         */
        for(JsonNode output : response.path("output"))
        {
            /*
             * message 타입만 확인
             */
            if(!"message".equals(
                    output.path("type").asText()
            ))
            {
                continue;
            }

            /*
             * message의 content 배열 탐색
             */
            for(JsonNode content : output.path("content"))
            {
                /*
                 * output_text 찾기
                 */
                if("output_text".equals(
                        content.path("type").asText()
                ))
                {
                    return content.path("text")
                            .asText();
                }
            }
        }

        throw new IllegalStateException(
                "OpenAI 응답에서 output_text를 찾지 못했습니다."
        );
    }

    /*
     * =========================
     * 혹시 LLM이
     *
     * ```json
     * {...}
     * ```
     *
     * 형태로 응답했을 경우 제거
     * =========================
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

}
