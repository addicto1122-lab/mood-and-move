package com.moodandmove.place.service;

import com.moodandmove.place.client.KakaoPlaceClient;
import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceResponse;
import com.moodandmove.place.filter.PlaceFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final KakaoPlaceClient kakaoPlaceClient;
    private final PlaceFilter placeFilter;

    public List<PlaceResponse> findNearbyPlaces(
            PlaceType placeType,
            double latitude,
            double longitude,
            int radius
    )
    {
        return switch(placeType)
        {
            case PARK -> kakaoPlaceClient.searchByKeyword(
                    "공원",
                    latitude,
                    longitude,
                    radius
            );

            case CAFE -> kakaoPlaceClient.searchByCategory(
                    "CE7",
                    latitude,
                    longitude,
                    radius
            );

            case SHOPPING -> kakaoPlaceClient.searchByKeyword(
                    "쇼핑몰",
                    latitude,
                    longitude,
                    radius
            );

            case LIBRARY -> kakaoPlaceClient.searchByKeyword(
                    "도서관",
                    latitude,
                    longitude,
                    radius
            );

            case CINEMA -> kakaoPlaceClient.searchByKeyword(
                    "영화관",
                    latitude,
                    longitude,
                    radius
            );
        };
    }

//    public List<PlaceResponse> findNearbyPlaceWithFallback(
//            PlaceType placeType,
//            double latitude,
//            double longitude
//    )
//    {
//        int[] radiuses = {3000,4000,5000};
//
//        for(int radius : radiuses)
//        {
//            System.out.println("장소 검색 반경 = " + radius + "m");
//
//            List<PlaceResponse> candidates = findNearbyPlaces(
//                    placeType,
//                    latitude,
//                    longitude,
//                    radius
//            );
//
//            if(!candidates.isEmpty())
//            {
//                continue;
//            }
//
//            /*
//             * 2. Gemini가 실제 목적에 맞는 장소만 필터링
//             */
//            List<PlaceResponse> filteredPlaces =
//                    placeFilter.filter(
//                            placeType,
//                            candidates
//                    );
//
//            /*
//             * 3. 적절한 장소가 있으면 반환
//             */
//            if (!filteredPlaces.isEmpty()) {
//                return filteredPlaces;
//            }
//        }
//        return List.of();
//    }
public List<PlaceResponse> findNearbyPlaceWithFallback(
        PlaceType placeType,
        double latitude,
        double longitude
) {
    int[] radiuses = {3000, 4000, 5000};

    for (int radius : radiuses) {

        System.out.println();
        System.out.println("==============================");
        System.out.println("장소 검색 반경 = " + radius + "m");
        System.out.println("placeType = " + placeType);

        /*
         * 1. Kakao 후보 검색
         */
        List<PlaceResponse> candidates =
                findNearbyPlaces(
                        placeType,
                        latitude,
                        longitude,
                        radius
                );

        System.out.println(
                "Kakao 후보 개수 = " + candidates.size()
        );

        candidates.forEach(place ->
                System.out.println(
                        "[KAKAO] "
                                + place.name()
                                + " / "
                                + place.category()
                                + " / "
                                + place.distance()
                                + "m"
                )
        );

        if (candidates.isEmpty()) {
            System.out.println("Kakao 후보 없음 → 다음 반경");
            continue;
        }

        /*
         * 2. 명백히 잘못된 장소 제거
         */
        List<PlaceResponse> cleanedCandidates =
                removeObviousMismatches(
                        placeType,
                        candidates
                );

        System.out.println(
                "1차 필터 후 개수 = "
                        + cleanedCandidates.size()
        );

        cleanedCandidates.forEach(place ->
                System.out.println(
                        "[CLEAN] "
                                + place.name()
                                + " / "
                                + place.category()
                )
        );

        if (cleanedCandidates.isEmpty()) {
            System.out.println("1차 필터에서 전부 제거 → 다음 반경");
            continue;
        }

        /*
         * 3. Gemini 최종 필터
         */
        List<PlaceResponse> filteredPlaces =
                placeFilter.filter(
                        placeType,
                        cleanedCandidates
                );

        System.out.println(
                "Gemini 필터 후 개수 = "
                        + filteredPlaces.size()
        );

        filteredPlaces.forEach(place ->
                System.out.println(
                        "[FINAL] "
                                + place.name()
                                + " / "
                                + place.distance()
                                + "m"
                )
        );

        if (!filteredPlaces.isEmpty()) {
            return filteredPlaces;
        }

        System.out.println("최종 장소 없음 → 다음 반경");
    }

    System.out.println("5km까지 적절한 장소 없음");

    return List.of();
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

                    String category =
                            place.category() == null
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
