
package com.moodandmove.recommendation.domain.type;

public enum RecommendationStatus {
    PENDING,     // 추천 생성 후 대기
    SELECTED,    // 사용자가 선택
    UNSELECTED,  // 다른 행동을 선택하여 미선택 처리
    SKIPPED      // 사용자가 추천을 건너뜀
}
