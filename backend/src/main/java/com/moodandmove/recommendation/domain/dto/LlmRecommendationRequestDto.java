package com.moodandmove.recommendation.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LlmRecommendationRequestDto {
    private CurrentStateDto currentState;
    private UserPreferenceDto preference;
    private List<ActionCandidateDto> candidates;
}
