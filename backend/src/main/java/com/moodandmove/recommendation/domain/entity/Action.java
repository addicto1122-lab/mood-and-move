
package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 행동 이름 (LLM 생성 행동의 중복 저장 방지)
    @Column(name = "action_name", nullable = false, unique = true, length = 100)
    private String actionName;

    // 행동 카테고리 (13개)
    @Column(name = "category", nullable = false, length = 30)
    private String category;

    // 예상 소요 시간
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // 활동 장소 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 20)
    private EnvironmentType environmentType = EnvironmentType.ANY;

    // 혼자 / 함께
    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType = SocialType.ANY;

    // 활동 성향
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_style", nullable = false, length = 20)
    private ActivityStyle activityStyle = ActivityStyle.ANY;

    // 실제 장소 검색이 필요한 행동인지
    @Column(name = "location_required", nullable = false)
    private boolean locationRequired = false;

    // 장소 검색에 사용하는 카테고리
    @Column(name = "place_category", length = 50)
    private String placeCategory;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    public static Action create(
            String actionName,
            String category,
            Integer durationMinutes,
            EnvironmentType environmentType,
            SocialType socialType,
            ActivityStyle activityStyle,
            boolean locationRequired,
            String placeCategory
    ) {
        Action action = new Action();

        action.actionName = actionName;
        action.category = category;
        action.durationMinutes = durationMinutes;
        action.environmentType = environmentType;
        action.socialType = socialType;
        action.activityStyle = activityStyle;
        action.locationRequired = locationRequired;
        action.placeCategory = placeCategory;

        return action;
    }
}
