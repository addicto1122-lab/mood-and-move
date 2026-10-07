package com.moodandmove.place.filter;

import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceResponse;

import java.util.List;

// BKW LLM TEST
public interface PlaceFilter {
    List<PlaceResponse> filter(
            PlaceType placeType,
            List<PlaceResponse> candidates
    );
}
