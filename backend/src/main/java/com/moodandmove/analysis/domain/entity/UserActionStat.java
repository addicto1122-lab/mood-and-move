package com.moodandmove.analysis.domain.entity;


import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_action_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_action_stats",
                columnNames = {"user_id", "action_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActionStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(name = "recommendation_count", nullable = false)
    private Integer recommendationCount = 0;

    @Column(name = "execution_count", nullable = false)
    private Integer executionCount = 0;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;

    @Column(name = "recheck_count", nullable = false)
    private Integer recheckCount = 0;

    @Column(name = "positive_count", nullable = false)
    private Integer positiveCount = 0;

    @Column(name = "avg_delta", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgDelta = BigDecimal.ZERO;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}