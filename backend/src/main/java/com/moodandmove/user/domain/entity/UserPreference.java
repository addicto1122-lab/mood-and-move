package com.moodandmove.user.domain.entity;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "activity_style",
            nullable = false,
            length = 20
    )
    private ActivityStyle activityStyle = ActivityStyle.ANY;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "activity_environment",
            nullable = false,
            length = 20
    )
    private EnvironmentType activityEnvironment = EnvironmentType.ANY;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "social_preference",
            nullable = false,
            length = 20
    )
    private SocialType socialPreference = SocialType.ANY;

    @Column(name = "default_available_minutes")
    private Integer defaultAvailableMinutes;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    private UserPreference(User user) {
        this.user = user;
        this.activityStyle = ActivityStyle.ANY;
        this.activityEnvironment = EnvironmentType.ANY;
        this.socialPreference = SocialType.ANY;
        this.defaultAvailableMinutes = null;
    }

    public static UserPreference createDefault(User user) {
        return new UserPreference(user);
    }


    public void updatePreference(
            ActivityStyle activityStyle,
            EnvironmentType activityEnvironment,
            SocialType socialPreference,
            Integer defaultAvailableMinutes
    ) {
        this.activityStyle = activityStyle;
        this.activityEnvironment = activityEnvironment;
        this.socialPreference = socialPreference;
        this.defaultAvailableMinutes = defaultAvailableMinutes;
    }
}