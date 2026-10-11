package com.moodandmove.place.client;

import com.moodandmove.place.client.dto.KakaoPlaceDocument;
import com.moodandmove.place.client.dto.KakaoPlaceSearchResponse;
import com.moodandmove.place.dto.PlaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class KakaoPlaceClient {

    private final RestClient restClient;

    public KakaoPlaceClient(
            @Value("${kakao.rest-api-key}") String restApiKey
    ){
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "KakaoAK " + restApiKey
                )
                .build();
    }

    /*
     * 키워드 기반 장소 검색
     *
     * 공원, 쇼핑몰, 도서관 등
     */
    public List<PlaceResponse> searchByKeyword(
            String keyword,
            double latitude,
            double longitude,
            int radius
    ){
        KakaoPlaceSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam(
                                "query"
                                ,keyword)
                        .queryParam(
                                "x",
                                longitude
                        )
                        .queryParam(
                                "y",
                                latitude
                        )
                        .queryParam(
                                "radius",
                                radius
                        )
                        .queryParam(
                                "sort",
                                "distance"
                        )
                        .queryParam(
                                "size",
                                5
                        )
                        .build()
                )
                .retrieve()
                .body(
                        KakaoPlaceSearchResponse.class
                );

        if(response == null || response.documents() == null)
        {
            return Collections.emptyList();
        }

        return response.documents()
                .stream()
                .map(this::toPlaceResponse)
                .toList();
    }

    /*
     * 카테고리 기반 장소 검색
     *
     * 예:
     * CE7 = 카페
     */
    public List<PlaceResponse> searchByCategory(
            String categoryCode,
            double latitude,
            double longitude,
            int radius
    ){
        KakaoPlaceSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/category.json")
                        .queryParam("category_group_code",
                        categoryCode
                        )
                        .queryParam(
                                "x",
                                longitude
                        )
                        .queryParam(
                                "y",
                                latitude
                        )
                        .queryParam(
                                "radius",
                                radius
                        )
                        .queryParam(
                                "sort",
                                "distance"
                        )
                        .queryParam(
                                "size",
                                5
                        )
                        .build()
                )
                .retrieve()
                .body(
                        KakaoPlaceSearchResponse.class
                );

        if(response == null || response.documents() == null)
        {
            return Collections.emptyList();
        }

        return response.documents()
                .stream()
                .map(this::toPlaceResponse)
                .toList();
    }

    /*
     * Kakao 응답 → 우리 서비스 응답
     */
    private PlaceResponse toPlaceResponse(
            KakaoPlaceDocument document
    ){
        return new PlaceResponse(
                document.id(),
                document.placeName(),
                document.categoryName(),
                document.addressName(),
                document.roadAddressName(),
                Double.valueOf(
                        document.y()
                ),
                Double.valueOf(
                        document.x()
                ),
                parseDistance(
                        document.distance()
                ),
                document.placeUrl()
        );
    }

    private Integer parseDistance(
            String distance
    )
    {
        if(distance == null || distance.isBlank())
        {
            return null;
        }

        return Integer.valueOf(distance);
    }
}
