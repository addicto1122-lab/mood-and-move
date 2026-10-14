package com.moodandmove.mood.controller;

import com.moodandmove.mood.dto.request.MoodCreateWithRecommendationRequest;
import com.moodandmove.mood.dto.response.MoodCreateWithRecommendationResponse;
import com.moodandmove.mood.service.MoodRecommendationService;
import com.moodandmove.mood.service.MoodService;
import com.moodandmove.user.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moods")
@RequiredArgsConstructor
public class MoodController {

    private final MoodService moodService;
    private final MoodRecommendationService moodRecommendationService;



    @PostMapping("/with-recommendations")
    public ResponseEntity<MoodCreateWithRecommendationResponse>
    createMoodWithRecommendations(
            Authentication authentication,
            @Valid @RequestBody MoodCreateWithRecommendationRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        var response = moodRecommendationService.create(
                user.getId(),
                request
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/today/exists")
    public boolean hasTodayMood(
            Authentication authentication
    )
    {
        User user =(User) authentication.getPrincipal();

        return moodService.hasTodayMood(
                user.getId()
        );
    }
}