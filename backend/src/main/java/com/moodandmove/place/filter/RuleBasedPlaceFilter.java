package com.moodandmove.place.filter;

import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedPlaceFilter {

    public List<PlaceResponse> filter(
            PlaceType placeType,
            List<PlaceResponse> candidates,
            String keyword
    ) {
        if (placeType == null || candidates == null) {
            return List.of();
        }

        return candidates.stream()
                .filter(place -> place != null)
                .filter(place -> matchesType(placeType, place.category()))
                .filter(place -> matchesKeyword(place, keyword))
                .toList();
    }

    // 장소 이름이 아니라 카카오가 반환한 업종 분류로 확인
    private boolean matchesType(PlaceType type, String category) {
        if (category == null || category.isBlank()) {
            return false;
        }

        return switch (type) {
            case PARK -> category.contains("공원");
            case CAFE -> category.contains("카페");
            case SHOPPING ->
                    category.contains("쇼핑센터")
                            || category.contains("백화점")
                            || category.contains("복합쇼핑몰")
                            || category.contains("아울렛");
            case LIBRARY -> category.contains("도서관");
            case CINEMA -> category.contains("영화관");
            case RESTAURANT ->
                    category.contains("음식점")
                            && !category.contains("카페")
                            && !category.contains("주점")
                            && !category.contains("술집");
            case PERFORMANCE ->
                    category.contains("공연")
                            || category.contains("극장")
                            && !category.contains("영화관");
            case MUSEUM -> category.contains("박물관");
            case ART_GALLERY ->
                    category.contains("미술관")
                            || category.contains("갤러리")
                            || category.contains("화랑");
            case STADIUM ->
                    category.contains("경기장")
                            || category.contains("야구장")
                            || category.contains("축구장");
        };
    }

    // 구체적인 검색어가 있으면 이름 또는 업종에도 근거가 있어야 통과
    private boolean matchesKeyword(PlaceResponse place, String keyword) {
        String normalizedKeyword = normalize(keyword);

        if (normalizedKeyword.isEmpty()) {
            return true;
        }

        return normalize(place.name()).contains(normalizedKeyword)
                || normalize(place.category()).contains(normalizedKeyword);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.strip()
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }
}