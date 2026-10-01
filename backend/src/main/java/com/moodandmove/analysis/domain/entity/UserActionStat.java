package com.moodandmove.analysis.domain.entity;

import com.moodandmove.analysis.domain.type.ConfidenceLevel;
import com.moodandmove.mood.domain.entity.Emotion;
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
                columnNames = {
                        "user_id",
                        "action_id",
                        "emotion_code",
                        "stat_year",
                        "stat_month"
                }
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emotion_code", nullable = false)
    private Emotion emotion;

    @Column(name = "stat_year", nullable = false)
    private Integer statYear;

    @Column(name = "stat_month", nullable = false)
    private Integer statMonth;

    // 추천된 횟수
    @Column(name = "recommendation_count", nullable = false)
    private Integer recommendationCount = 0;

    // 사용자가 실제 행동을 선택/시작한 횟수
    @Column(name = "execution_count", nullable = false)
    private Integer executionCount = 0;

    // 행동 완료 + 재측정까지 끝난 유효 데이터 수
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount = 0;

    // 재측정 결과 기분점수가 상승한 횟수
    @Column(name = "positive_count", nullable = false)
    private Integer positiveCount = 0;

    @Column(name = "positive_rate", precision = 5, scale = 2)
    private BigDecimal positiveRate;

    @Column(name = "avg_delta", precision = 5, scale = 2)
    private BigDecimal avgDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", length = 20)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}