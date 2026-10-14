package com.moodandmove.place.controller;

import com.moodandmove.place.dto.PlaceResponse;
import com.moodandmove.place.service.PlaceService;
import com.moodandmove.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/nearby")
    public List<PlaceResponse> getNearbyPlaces(
            Authentication authentication,
            @RequestParam("recommendationId") Long recommendationId
    ) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return placeService.findPlacesForRecommendation(
                user.getId(),
                recommendationId
        );
    }
}