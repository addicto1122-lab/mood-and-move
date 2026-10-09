package com.moodandmove.mood.controller;

import com.moodandmove.mood.dto.request.MoodCreateRequest;
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

    @PostMapping
    public ResponseEntity<Long> createMood(
            Authentication authentication,
            @Valid @RequestBody MoodCreateRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        Long moodEntryId = moodService.createMood(
                user.getId(),
                request
        );

        return ResponseEntity.ok(moodEntryId);
    }
}