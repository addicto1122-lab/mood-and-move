package com.moodandmove.analysis.repository.projection;

public interface BestActionProjection {

    Long getActionId();

    String getActionName();

    String getCategory();

    Integer getDelta();
}
