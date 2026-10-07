package com.moodandmove.place.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoPlaceDocument(
        String id,
        @JsonProperty("place_name")
        String placeName,

        @JsonProperty("category_name")
        String categoryName,

        @JsonProperty("address_name")
        String addressName,

        @JsonProperty("road_address_name")
        String roadAddressName,

        String x,

        String y,

        String distance,

        @JsonProperty("place_url")
        String placeUrl
) {
}
