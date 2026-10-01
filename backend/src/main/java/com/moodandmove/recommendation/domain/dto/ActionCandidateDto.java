package com.moodandmove.recommendation.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ActionCandidateDto {

    private String actionCode;
    private String actionName;
    private BigDecimal score;
}
