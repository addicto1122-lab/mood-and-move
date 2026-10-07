package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.recommendation.domain.type.RecommendationType;
import com.moodandmove.recommendation.domain.type.SelectionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "mood_entry_id",
            nullable = false,
            unique = true
    )
    private MoodEntry moodEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 20)
    private RecommendationType recommendationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_bucket", length = 20)
    private TimeBucket timeBucket;

    @Column(name = "available_minutes")
    private Integer availableMinutes;

    @Column(name = "weather_condition", length = 30)
    private String weatherCondition;

    @Column(name = "temperature_celsius", precision = 4, scale = 1)
    private BigDecimal temperatureCelsius;

    @Column(name = "precipitation_mm", precision = 6, scale = 2)
    private BigDecimal precipitationMm;

    @Column(name = "weather_observed_at")
    private LocalDateTime weatherObservedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_status", nullable = false, length = 20)
    private SelectionStatus selectionStatus = SelectionStatus.GENERATED;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;
    public static RecommendationSession create(
            MoodEntry moodEntry,
            TimeBucket timeBucket,
            RecommendationType recommendationType
    ) {
        RecommendationSession session = new RecommendationSession();

        session.moodEntry = moodEntry;
        session.timeBucket = timeBucket;
        session.recommendationType = recommendationType;

        return session;
    }
}