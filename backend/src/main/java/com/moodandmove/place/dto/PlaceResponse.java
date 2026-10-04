package com.moodandmove.place.dto;

public record PlaceResponse(
        String placeId,
        String name,
        String category,
        String address,
        String roadAddress,
        Double latitude,
        Double longitude,
        Integer distance,
        String placeUrl
){
}
