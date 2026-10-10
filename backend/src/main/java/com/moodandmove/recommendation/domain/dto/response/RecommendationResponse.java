package com.moodandmove.recommendation.domain.dto.response;

import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.recommendation.domain.entity.ActionCategory;
import com.moodandmove.recommendation.domain.entity.Recommendation;

import java.util.List;

public record RecommendationResponse(
        Long sessionId,
        List<Item> recommendations
) {

    public record Item(
            Long recommendationId,
            Long actionId,
            String actionName,
            Integer durationMinutes,
            EnvironmentType environmentType,
            boolean locationRequired,
            Integer rankNo,
            String reason,
            String category,
            String categoryName,
            String emoji,
            String categoryEmoji
    ) {

        // 기존 호출부도 컴파일되도록 유지
        public static Item from(Recommendation recommendation) {
            return from(recommendation, null);
        }

        // DB에서 조회한 카테고리 정보까지 응답에 포함
        public static Item from(
                Recommendation recommendation,
                ActionCategory actionCategory
        ) {
            var action = recommendation.getAction();

            return new Item(
                    recommendation.getId(),
                    action.getId(),
                    action.getActionName(),
                    action.getDurationMinutes(),
                    action.getEnvironmentType(),
                    action.isLocationRequired(),
                    recommendation.getRankNo(),
                    recommendation.getReason(),
                    action.getCategory(),
                    actionCategory != null
                            ? actionCategory.getCategoryName()
                            : null,
                    recommendation.getEmoji(),
                    actionCategory != null
                            ? actionCategory.getEmoji()
                            : null
            );
        }
    }
}