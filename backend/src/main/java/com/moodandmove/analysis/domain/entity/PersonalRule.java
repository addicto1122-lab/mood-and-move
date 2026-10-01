package com.moodandmove.analysis.domain.entity;


import com.moodandmove.analysis.domain.type.ConfidenceLevel;
import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.domain.entity.ActivityTag;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(name = "mood_min")
    private Integer moodMin;

    @Column(name = "mood_max")
    private Integer moodMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_bucket", length = 20)
    private TimeBucket timeBucket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_tag_id")
    private ActivityTag activityTag;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount = 0;

    @Column(name = "positive_count", nullable = false)
    private Integer positiveCount = 0;

    @Column(name = "positive_rate", precision = 5, scale = 2)
    private BigDecimal positiveRate;

    @Column(name = "avg_delta", precision = 5, scale = 2)
    private BigDecimal avgDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", length = 20)
    private ConfidenceLevel confidenceLevel;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}