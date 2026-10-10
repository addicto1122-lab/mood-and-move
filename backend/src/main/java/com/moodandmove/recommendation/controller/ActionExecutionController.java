package com.moodandmove.recommendation.controller;

import com.moodandmove.recommendation.domain.dto.MoodRecheckRequest;
import com.moodandmove.recommendation.domain.dto.RecommendationSelectRequest;
import com.moodandmove.recommendation.domain.dto.response.ActionExecutionResponse;
import com.moodandmove.recommendation.domain.dto.response.RecommendationResponse;
import com.moodandmove.recommendation.service.ActionExecutionService;
import com.moodandmove.user.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActionExecutionController {

    private final ActionExecutionService actionExecutionService;

    // 행동 선택: 선택 즉시 실행 시작
    @PostMapping("/recommendations/sessions/{sessionId}/select")
    public ResponseEntity<ActionExecutionResponse> select(
            Authentication authentication,
            @PathVariable Long sessionId,
            @Valid @RequestBody RecommendationSelectRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                actionExecutionService.select(
                        user.getId(),
                        sessionId,
                        request.recommendationId()
                )
        );
    }

    // 추천 3개 모두 건너뛰기
    @PostMapping("/recommendations/sessions/{sessionId}/skip")
    public ResponseEntity<RecommendationResponse> skip(
            Authentication authentication,
            @PathVariable Long sessionId
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                actionExecutionService.skip(
                        user.getId(),
                        sessionId
                )
        );
    }

    // 실행 기록과 재측정 상태 조회
    @GetMapping("/action-executions/{executionId}")
    public ResponseEntity<ActionExecutionResponse> get(
            Authentication authentication,
            @PathVariable Long executionId
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                actionExecutionService.get(
                        user.getId(),
                        executionId
                )
        );
    }

    // 슬라이더로 입력한 기분 점수 저장
    @PostMapping("/action-executions/{executionId}/recheck")
    public ResponseEntity<ActionExecutionResponse> recheck(
            Authentication authentication,
            @PathVariable Long executionId,
            @Valid @RequestBody MoodRecheckRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                actionExecutionService.recheck(
                        user.getId(),
                        executionId,
                        request.afterScore()
                )
        );
    }

    @GetMapping("/action-executions/current")
    public ResponseEntity<ActionExecutionResponse> getCurrent(
            Authentication authentication
    )
    {
        User user = (User) authentication.getPrincipal();

        return actionExecutionService.findCurrent(user.getId())
                .map(ResponseEntity::ok)
                .orElseGet(()-> ResponseEntity.noContent().build());
    }
}