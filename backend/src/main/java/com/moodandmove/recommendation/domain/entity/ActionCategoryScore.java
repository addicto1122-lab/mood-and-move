
package com.moodandmove.recommendation.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "action_category_scores",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_action_category_scores_user_category",
                        columnNames = {"user_id", "category"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionCategoryScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 행동 카테고리 (13개)
    @Column(name = "category", nullable = false, length = 30)
    private String category;

    // 개인화 점수 (0~100, 기본값 50)
    @Column(
            name = "personal_score",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal personalScore = new BigDecimal("50.00");

    // 해당 카테고리의 평균 기분 변화량 (-59~59)
    @Column(
            name = "avg_delta",
            precision = 6,
            scale = 2
    )
    private BigDecimal avgDelta;

    // 긍정적인 기분 변화를 보인 횟수
    @Column(name = "positive_count", nullable = false)
    private Integer positiveCount = 0;

    // 유효 재측정 표본 수
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount = 0;

    // 마지막 통계 수정 시각
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 카테고리 통계 최초 생성
    public static ActionCategoryScore create(
            Long userId,
            String category
    ) {
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }

        ActionCategoryScore score = new ActionCategoryScore();

        score.userId = userId;
        score.category = category;
        score.personalScore = new BigDecimal("50.00");
        score.avgDelta = null;
        score.positiveCount = 0;
        score.sampleCount = 0;

        return score;
    }

    // 재측정 통계 계산 후 결과 반영
    public void updateStatistics(
            BigDecimal personalScore,
            BigDecimal avgDelta,
            int positiveCount,
            int sampleCount
    ) {
        Objects.requireNonNull(personalScore, "개인화 점수는 필수입니다.");

        if (personalScore.compareTo(BigDecimal.ZERO) < 0
                || personalScore.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "개인화 점수는 0~100 사이여야 합니다."
            );
        }

        if (avgDelta != null
                && (avgDelta.compareTo(new BigDecimal("-59")) < 0
                || avgDelta.compareTo(new BigDecimal("59")) > 0)) {
            throw new IllegalArgumentException(
                    "평균 기분 변화량은 -59~59 사이여야 합니다."
            );
        }

        if (sampleCount < 0 || positiveCount < 0
                || positiveCount > sampleCount) {
            throw new IllegalArgumentException(
                    "통계 횟수가 올바르지 않습니다."
            );
        }

        this.personalScore = personalScore;
        this.avgDelta = avgDelta;
        this.positiveCount = positiveCount;
        this.sampleCount = sampleCount;
    }
}
