package com.moodandmove.recommendation.domain.dto.response;

import com.moodandmove.analysis.domain.entity.MoodRecheck;
import com.moodandmove.recommendation.domain.entity.ActionExecution;
import com.moodandmove.recommendation.domain.type.ExecutionStatus;
import com.moodandmove.recommendation.domain.entity.ActionCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActionExecutionResponse(
        Long executionId,
        Long sessionId,
        Long moodEntryId,
        LocalDate entryDate,
        Long recommendationId,
        String actionName,
        String category,
        Integer durationMinutes,
        String reason,
        ExecutionStatus status,
        LocalDateTime startedAt,
        LocalDateTime recheckedAt,
        Integer beforeScore,
        Integer afterScore,
        Integer delta,
        String emoji,
        String categoryName,
        String categoryEmoji
) {
    // 기존 호출부도 사용할 수 있도록 유지
    public static ActionExecutionResponse from(
            ActionExecution execution,
            MoodRecheck recheck
    ) {
        return from(execution, recheck, null);
    }
    // 카테고리 정보까지 포함하는 응답
    public static ActionExecutionResponse from(
            ActionExecution execution,
            MoodRecheck recheck,
            ActionCategory actionCategory
    ) {
        var session = execution.getSession();
        var recommendation = execution.getRecommendation();
        var action = recommendation.getAction();

        return new ActionExecutionResponse(
                execution.getId(),
                session.getId(),
                session.getMoodEntry().getId(),
                session.getMoodEntry().getEntryDate(),
                recommendation.getId(),
                action.getActionName(),
                action.getCategory(),
                action.getDurationMinutes(),
                recommendation.getReason(),
                execution.getStatus(),
                execution.getStartedAt(),
                execution.getRecheckedAt(),
                recheck.getBeforeScore(),
                recheck.getAfterScore(),
                recheck.getDelta(),
                recommendation.getEmoji(),
                actionCategory != null
                        ? actionCategory.getCategoryName()
                        : null,
                actionCategory != null
                        ? actionCategory.getEmoji()
                        : null
        );
    }
}