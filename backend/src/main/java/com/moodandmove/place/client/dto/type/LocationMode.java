package com.moodandmove.place.client.dto.type;

import java.math.BigDecimal;

public record LocationMode(
        LocationMode locationMode,
        BigDecimal latitude,
        BigDecimal longitude

) {
}
