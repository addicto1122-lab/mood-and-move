package com.moodandmove.recommendation.llm;

import com.moodandmove.recommendation.domain.dto.LlmRecommendationRequestDto;
import com.moodandmove.recommendation.domain.dto.LlmRecommendationResult;

public interface RecommendationLlmGenerator {
    LlmRecommendationResult generate(
            LlmRecommendationRequestDto request
    );
}
