package com.moodandmove.recommendation.domain.dto;



import java.math.BigDecimal;

public record ActionCandidateDto (
        String actionCode,
        String actionName,
        BigDecimal score
){


}
