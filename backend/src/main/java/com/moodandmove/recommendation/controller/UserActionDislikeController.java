package com.moodandmove.recommendation.controller;

import com.moodandmove.recommendation.domain.dto.response.DislikeActionResponse;
import com.moodandmove.recommendation.service.UserActionDislikeService;
import com.moodandmove.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/dislikes")
@RequiredArgsConstructor
public class UserActionDislikeController {

    private final UserActionDislikeService userActionDislikeService;

    @GetMapping
    public ResponseEntity<List<DislikeActionResponse>> getDislikes(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                userActionDislikeService.getDislikeActions(
                        user.getId()
                )
        );
    }

    @PostMapping("/{actionId}")
    public ResponseEntity<Void> addDislike(
            Authentication authentication,
            @PathVariable Long actionId
    ) {
        User user = (User) authentication.getPrincipal();

        try {
            userActionDislikeService.addDislike(
                    user.getId(),
                    actionId
            );

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{actionId}")
    public ResponseEntity<Void> removeDislike(
            Authentication authentication,
            @PathVariable Long actionId
    ) {
        User user = (User) authentication.getPrincipal();

        userActionDislikeService.removeDislike(
                user.getId(),
                actionId
        );

        return ResponseEntity.ok().build();
    }
}