package com.moodandmove.place.controller;

import com.moodandmove.place.domain.type.PlaceType;
import com.moodandmove.place.dto.PlaceResponse;
import com.moodandmove.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {
    private final PlaceService placeService;

    @GetMapping("/nearby")
    public List<PlaceResponse> getNearbyPlaces(
            @RequestParam PlaceType type,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "3000")
            int radius
    ){
        System.out.println("======= PlaceController 호출됨 =======");
        return placeService.findNearbyPlaces(
                type,
                latitude,
                longitude,
                radius
        );
    }
}
