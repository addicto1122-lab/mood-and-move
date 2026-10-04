package com.moodandmove.user.controller;

import com.moodandmove.user.dto.response.LocationSearchResponse;
import com.moodandmove.user.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/search")
    public ResponseEntity<List<LocationSearchResponse>> searchRegion(
            @RequestParam String query
    ) {

        return ResponseEntity.ok(
                locationService.searchRegion(query)
        );
    }
}