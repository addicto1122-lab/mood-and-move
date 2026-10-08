
package com.moodandmove.recommendation.domain.dto.response;

import com.moodandmove.common.domain.type.EnvironmentType;
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
            String reason
    ) {

        public static Item from(Recommendation recommendation) {
            var action = recommendation.getAction();

            return new Item(
                    recommendation.getId(),
                    action.getId(),
                    action.getActionName(),
                    action.getDurationMinutes(),
                    action.getEnvironmentType(),
                    action.isLocationRequired(),
                    recommendation.getRankNo(),
                    recommendation.getReason()
            );
        }
    }
}
