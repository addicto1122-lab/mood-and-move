
package com.moodandmove.recommendation.controller;

import com.moodandmove.recommendation.domain.dto.RecommendationGenerateRequest;
import com.moodandmove.recommendation.domain.dto.response.RecommendationResponse;
import com.moodandmove.recommendation.service.RecommendationPersistenceService;
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
    private final RecommendationPersistenceService persistenceService;

    // 추천 생성
    @PostMapping("/{moodEntryId}/generate")
    public ResponseEntity<RecommendationResponse> generate(
            Authentication authentication,
            @PathVariable Long moodEntryId,
            @RequestBody RecommendationGenerateRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        RecommendationResponse result =
                recommendationService.generateRecommendations(
                        user.getId(),
                        moodEntryId,
                        request
                );

        return ResponseEntity.ok(result);
    }

    // 저장된 추천 조회 (새로고침 및 URL 직접 접근)
    @GetMapping("/mood-entries/{moodEntryId}")
    public ResponseEntity<RecommendationResponse> getRecommendation(
            Authentication authentication,
            @PathVariable Long moodEntryId
    ) {
        User user = (User) authentication.getPrincipal();

        return persistenceService
                .findExisting(user.getId(), moodEntryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
