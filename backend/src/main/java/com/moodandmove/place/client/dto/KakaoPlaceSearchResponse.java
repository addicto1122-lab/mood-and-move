package com.moodandmove.place.client.dto;

import java.util.List;

public record KakaoPlaceSearchResponse(
        List<KakaoPlaceDocument> documents
) {
}
