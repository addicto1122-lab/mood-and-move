package com.moodandmove.place.service;

import com.moodandmove.place.client.KakaoPlaceClient;
import com.moodandmove.place.domain.type.LocationMode;
import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceResponse;
import com.moodandmove.place.filter.PlaceFilter;
import com.moodandmove.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final KakaoPlaceClient kakaoPlaceClient;
    private final PlaceFilter placeFilter;
    private final RecommendationRepository recommendationRepository;

    public List<PlaceResponse> findPlacesForRecommendation(
            Long userId,
            Long recommendationId
    ) {
        // 본인 소유의 추천만 조회
        var recommendation = recommendationRepository
                .findByIdAndSession_UserId(recommendationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "추천을 찾을 수 없습니다."
                ));

        var session = recommendation.getSession();

        // 위치 사용 안 함이면 외부 장소 API를 호출하지 않음
        if (session.getLocationMode() != LocationMode.CURRENT
                && session.getLocationMode() != LocationMode.SAVED) {
            return List.of();
        }

        // 장소 검색 대상이 아닌 행동
        if (!recommendation.isLocationRequired()) {
            return List.of();
        }

        // 과거 추천처럼 작성 당시 좌표가 없는 경우
        if (session.getSearchLatitude() == null
                || session.getSearchLongitude() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이 추천에는 작성 당시 위치가 저장되어 있지 않습니다."
            );
        }

        String placeCategory = recommendation.getPlaceCategory();

        if (placeCategory == null || placeCategory.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이 추천에 장소 검색 유형이 저장되어 있지 않습니다."
            );
        }

        PlaceType placeType;

        try {
            placeType = PlaceType.valueOf(placeCategory);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "지원하지 않는 장소 검색 유형입니다."
            );
        }

        // 좌표는 세션에서, 장소 유형과 검색어는 해당 추천에서 가져옴
        return findNearbyPlaceWithFallback(
                placeType,
                session.getSearchLatitude().doubleValue(),
                session.getSearchLongitude().doubleValue(),
                recommendation.getPlaceKeyword()
        );
    }

    public List<PlaceResponse> findNearbyPlaces(
            PlaceType placeType,
            double latitude,
            double longitude,
            int radius,
            String keyword
    ) {
        // 구체적인 검색어가 있으면 우선 사용
        if (keyword != null) {
            return kakaoPlaceClient.searchByKeyword(
                    keyword,
                    latitude,
                    longitude,
                    radius
            );
        }

        // 검색어가 없는 카페 요청은 기존 방식 유지
        if (placeType == PlaceType.CAFE) {
            return kakaoPlaceClient.searchByCategory(
                    "CE7",
                    latitude,
                    longitude,
                    radius
            );
        }

        String defaultKeyword = switch (placeType) {
            case PARK -> "공원";
            case SHOPPING -> "쇼핑몰";
            case LIBRARY -> "도서관";
            case CINEMA -> "영화관";
            case RESTAURANT -> "음식점";
            case PERFORMANCE -> "공연장";
            case MUSEUM -> "박물관";
            case ART_GALLERY -> "미술관";
            case STADIUM -> "경기장";
            case CAFE -> throw new IllegalStateException(
                    "카페는 카테고리 검색을 사용합니다."
            );
        };

        return kakaoPlaceClient.searchByKeyword(
                defaultKeyword,
                latitude,
                longitude,
                radius
        );
    }

    public List<PlaceResponse> findNearbyPlaceWithFallback(
            PlaceType placeType,
            double latitude,
            double longitude,
            String keyword
    ) {
        if (placeType == null
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "장소 검색 위치가 올바르지 않습니다."
            );
        }

        String searchKeyword = normalizeKeyword(keyword);
        int radius = 5000;

        // 기준 위치에서 최대 5km 이내 후보 검색
        List<PlaceResponse> candidates = findNearbyPlaces(
                placeType,
                latitude,
                longitude,
                radius,
                searchKeyword
        );

        // 거리 정보가 없거나 5km를 초과하는 후보는 제외
        List<PlaceResponse> nearbyCandidates = candidates.stream()
                .filter(place ->
                        place.distance() != null
                                && place.distance() >= 0
                                && place.distance() <= radius
                )
                .sorted(Comparator.comparing(PlaceResponse::distance))
                .toList();

        List<PlaceResponse> cleanedCandidates =
                removeObviousMismatches(
                        placeType,
                        nearbyCandidates
                );

        if (cleanedCandidates.isEmpty()) {
            return List.of();
        }

        // 검색된 후보 중 장소 유형에 맞는 것만 선택
        List<PlaceResponse> filteredPlaces =
                placeFilter.filter(
                        placeType,
                        cleanedCandidates,
                        searchKeyword
                );

        return filteredPlaces.stream()
                .filter(place ->
                        place.distance() != null
                                && place.distance() >= 0
                                && place.distance() <= radius
                )
                .sorted(Comparator.comparing(PlaceResponse::distance))
                .limit(5)
                .toList();
    }
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String normalized = keyword.strip().replaceAll("\\s+", " ");

        if (normalized.codePointCount(0, normalized.length()) > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "장소 검색어는 50자 이하여야 합니다."
            );
        }

        return normalized;
    }
    private List<PlaceResponse> removeObviousMismatches(
            PlaceType placeType,
            List<PlaceResponse> candidates
    ) {
        if (placeType != PlaceType.PARK) {
            return candidates;
        }

        return candidates.stream()
                .filter(place -> {
                    String category = place.category() == null
                            ? ""
                            : place.category();

                    return !category.contains("음식점")
                            && !category.contains("카페")
                            && !category.contains("술집")
                            && !category.contains("편의점")
                            && !category.contains("마트");
                })
                .toList();
    }
}
