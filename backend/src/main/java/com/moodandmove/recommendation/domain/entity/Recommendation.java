
package com.moodandmove.recommendation.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "recommendations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_recommendations_session_rank",
                        columnNames = {"session_id", "rank_no"}
                ),
                @UniqueConstraint(
                        name = "uq_recommendations_session_action",
                        columnNames = {"session_id", "action_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추천 세션
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private RecommendationSession session;

    // 추천 행동
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    // 추천 순위 (1~3)
    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    // LLM이 생성한 추천 이유
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    // 기분 재측정 완료 여부
    @Column(name = "recheck_completed", nullable = false)
    private boolean recheckCompleted = false;

    // PENDING / SELECTED / UNSELECTED / SKIPPED
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    // 사용자가 행동을 선택한 시각
    @Column(name = "selected_at")
    private LocalDateTime selectedAt;

    // 추천 생성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 추천 결과 생성
    public static Recommendation create(
            RecommendationSession session,
            Action action,
            int rankNo,
            String reason
    ) {
        if (rankNo < 1 || rankNo > 3) {
            throw new IllegalArgumentException("추천 순위는 1~3이어야 합니다.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("추천 이유는 필수입니다.");
        }

        Recommendation recommendation = new Recommendation();

        recommendation.session = session;
        recommendation.action = action;
        recommendation.rankNo = rankNo;
        recommendation.reason = reason;
        recommendation.status = "PENDING";
        recommendation.recheckCompleted = false;

        return recommendation;
    }

    // 추천 행동 선택
    public void select() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("대기 중인 추천만 선택할 수 있습니다.");
        }

        this.status = "SELECTED";
        this.selectedAt = LocalDateTime.now();
    }

    // 선택되지 않은 행동
    public void unselect() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("대기 중인 추천만 미선택 처리할 수 있습니다.");
        }

        this.status = "UNSELECTED";
    }

    // 추천 건너뛰기
    public void skip() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("대기 중인 추천만 건너뛸 수 있습니다.");
        }

        this.status = "SKIPPED";
    }

    // 기분 재측정 완료
    public void completeRecheck() {
        if (!"SELECTED".equals(this.status)) {
            throw new IllegalStateException("선택된 추천만 재측정을 완료할 수 있습니다.");
        }

        if (this.recheckCompleted) {
            throw new IllegalStateException("이미 재측정이 완료된 추천입니다.");
        }

        this.recheckCompleted = true;
    }
}
