package com.moodandmove.recommendation.domain.dto;



import java.util.List;


public record LlmRecommendationRequestDto(
        CurrentStateDto currentState,
        UserPreferenceDto preference,
        List<ActionCandidateDto> candidates

) {}

