package com.moodandmove.place.service;

import com.moodandmove.place.client.KakaoPlaceClient;
import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final KakaoPlaceClient kakaoPlaceClient;

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

    public List<PlaceResponse> findNearbyPlaceWithFallback(
            PlaceType placeType,
            double latitude,
            double longitude
    )
    {
        int[] radiuses = {3000,4000,5000};

        for(int radius : radiuses)
        {
            System.out.println("장소 검색 반경 = " + radius + "m");

            List<PlaceResponse> places = findNearbyPlaces(
                    placeType,
                    latitude,
                    longitude,
                    radius
            );

            if(!places.isEmpty())
            {
                return places;
            }
        }
        return List.of();
    }
}
