package com.moodandmove.user.service;

import com.moodandmove.user.dto.response.LocationSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    public List<LocationSearchResponse> searchRegion(String query) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "검색할 지역을 입력해주세요."
            );
        }

        RestClient restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .build();

        Map response = restClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/v2/local/search/address.json")
                                .queryParam("query", query.trim())
                                .build()
                )
                .header(
                        "Authorization",
                        "KakaoAK " + kakaoRestApiKey
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            return List.of();
        }

        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) response.get("documents");

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<LocationSearchResponse> results =
                new ArrayList<>();

        for (Map<String, Object> document : documents) {

            Map<String, Object> address =
                    (Map<String, Object>) document.get("address");

            if (address == null) {
                continue;
            }

            String region1 =
                    (String) address.get("region_1depth_name");

            String region2 =
                    (String) address.get("region_2depth_name");

            String region3H =
                    (String) address.get("region_3depth_h_name");

            String region3 =
                    (String) address.get("region_3depth_name");

            String regionName =
                    String.join(
                            " ",
                            region1,
                            region2,
                            region3H != null && !region3H.isBlank()
                                    ? region3H
                                    : region3
                    );

            String hCode =
                    (String) address.get("h_code");

            String bCode =
                    (String) address.get("b_code");

            String regionCode =
                    hCode != null && !hCode.isBlank()
                            ? hCode
                            : bCode;

            BigDecimal longitude =
                    new BigDecimal(
                            String.valueOf(address.get("x"))
                    );

            BigDecimal latitude =
                    new BigDecimal(
                            String.valueOf(address.get("y"))
                    );

            LocationSearchResponse result =
                    new LocationSearchResponse(
                            regionName,
                            regionCode,
                            latitude,
                            longitude
                    );

            boolean duplicate =
                    results.stream()
                            .anyMatch(item ->
                                    item.regionName()
                                            .equals(result.regionName())
                            );

            if (!duplicate) {
                results.add(result);
            }
        }

        return results;
    }
}