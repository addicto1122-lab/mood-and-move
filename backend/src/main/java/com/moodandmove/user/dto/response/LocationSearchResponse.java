package com.moodandmove.user.dto.response;

import java.math.BigDecimal;

public record LocationSearchResponse(

        String regionName,

        String regionCode,

        BigDecimal latitude,

        BigDecimal longitude

) {
}