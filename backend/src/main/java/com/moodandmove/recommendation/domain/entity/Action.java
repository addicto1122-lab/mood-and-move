package com.moodandmove.recommendation.domain.entity;


import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_code", nullable = false, unique = true, length = 50)
    private String actionCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 20)
    private EnvironmentType environmentType = EnvironmentType.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType = SocialType.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_style", nullable = false, length = 20)
    private ActivityStyle activityStyle = ActivityStyle.ANY;

    @Column(name = "location_required", nullable = false)
    private boolean locationRequired = false;

    @Column(name = "place_category", length = 50)
    private String placeCategory;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}