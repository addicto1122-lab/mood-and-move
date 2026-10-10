package com.moodandmove.analysis.repository.projection;

import java.math.BigDecimal;

public interface CategoryEffectRankProjection {

    String getCategory();

    BigDecimal getAverageDelta();

    Long getSampleCount();

}
