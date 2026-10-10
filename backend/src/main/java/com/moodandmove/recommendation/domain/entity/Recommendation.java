
package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.recommendation.domain.type.RecommendationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private RecommendationSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    // LLM이 생성한 추천 행동별 이모지
    @Column(name = "emoji")
    private String emoji;

    @Column(name = "recheck_completed", nullable = false)
    private boolean recheckCompleted = false;

    // Enum을 DB 문자열로 저장
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @Column(name = "selected_at")
    private LocalDateTime selectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 추천 생성
    public static Recommendation create(
            RecommendationSession session,
            Action action,
            int rankNo,
            String reason
    ) {
        Objects.requireNonNull(session, "추천 세션은 필수입니다.");
        Objects.requireNonNull(action, "추천 행동은 필수입니다.");

        if (rankNo < 1 || rankNo > 3) {
            throw new IllegalArgumentException(
                    "추천 순위는 1~3이어야 합니다."
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "추천 이유는 필수입니다."
            );
        }

        Recommendation recommendation = new Recommendation();

        recommendation.session = session;
        recommendation.action = action;
        recommendation.rankNo = rankNo;
        recommendation.reason = reason.strip();
        recommendation.status = RecommendationStatus.PENDING;
        recommendation.recheckCompleted = false;

        return recommendation;
    }

    // 추천 행동 선택: PENDING -> SELECTED
    public void select() {
        validatePending("선택");

        this.status = RecommendationStatus.SELECTED;
        this.selectedAt = LocalDateTime.now();
    }

    // 다른 행동이 선택된 경우: PENDING -> UNSELECTED
    public void unselect() {
        validatePending("미선택 처리");

        this.status = RecommendationStatus.UNSELECTED;
    }

    // 추천 건너뛰기: PENDING -> SKIPPED
    public void skip() {
        validatePending("건너뛰기");

        this.status = RecommendationStatus.SKIPPED;
    }

    // 기분 재측정 완료
    public void completeRecheck() {
        if (this.status != RecommendationStatus.SELECTED) {
            throw new IllegalStateException(
                    "선택된 추천만 재측정을 완료할 수 있습니다."
            );
        }

        if (this.recheckCompleted) {
            throw new IllegalStateException(
                    "이미 재측정이 완료된 추천입니다."
            );
        }

        this.recheckCompleted = true;
    }

    // PENDING 상태에서만 변경 가능
    private void validatePending(String operation) {
        if (this.status != RecommendationStatus.PENDING) {
            throw new IllegalStateException(
                    "대기 중인 추천만 " + operation + "할 수 있습니다."
            );
        }
    }
    // 추천 생성 시 이모지 설정
    public void assignEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            this.emoji = null;
            return;
        }

        String value = emoji.strip();

        // DB 컬럼 길이를 넘는 값은 저장하지 않음
        if (value.codePointCount(0, value.length()) > 32) {
            this.emoji = null;
            return;
        }

        this.emoji = value;
    }
}
