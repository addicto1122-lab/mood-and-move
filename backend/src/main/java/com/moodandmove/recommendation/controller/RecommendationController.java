package com.moodandmove.recommendation.controller;

import com.moodandmove.recommendation.domain.dto.LlmRecommendationResult;
import com.moodandmove.recommendation.domain.dto.RecommendationGenerateRequest;
import com.moodandmove.recommendation.llm.RecommendationLlmGenerator;
import com.moodandmove.recommendation.service.RecommendationService;
import com.moodandmove.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationLlmGenerator recommendationLlmGenerator;

    @PostMapping("/{moodEntryId}/generate")
    public ResponseEntity<LlmRecommendationResult>
    generate(
            Authentication authentication,
            @PathVariable Long moodEntryId,
            @RequestBody RecommendationGenerateRequest request
    ) {

        User user =
                (User) authentication
                        .getPrincipal();


        LlmRecommendationResult result =
                recommendationService
                        .generateRecommendations(
                                user.getId(),
                                moodEntryId,
                                request
                        );


        return ResponseEntity.ok(result);
    }
}