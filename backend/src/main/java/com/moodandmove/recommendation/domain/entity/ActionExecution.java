
package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.recommendation.domain.type.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "action_executions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 실행한 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 세션당 실행은 최대 1개
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "session_id",
            nullable = false,
            unique = true
    )
    private RecommendationSession session;

    // 사용자가 선택하여 실행한 추천
    // DB에서는 (session_id, recommendation_id)
    // 복합 FK로 동일 세션 소속 여부 검증
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recommendation_id",
            nullable = false,
            unique = true
    )
    private Recommendation recommendation;

    // STARTED, COMPLETED, CANCELED
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    // 실행 시작 시각
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;


    // 기분 재측정 완료 시각
    @Column(name = "rechecked_at")
    private LocalDateTime recheckedAt;

    // 행동 선택 즉시 실행 시작
    public static ActionExecution select(
            Long userId,
            Recommendation recommendation
    ) {
        ActionExecution execution = new ActionExecution();

        execution.userId = userId;
        execution.session = recommendation.getSession();
        execution.recommendation = recommendation;
        execution.status = ExecutionStatus.STARTED;
        execution.startedAt = LocalDateTime.now();

        return execution;
    }

    // 기분 재측정이 끝나면 실행 완료
    public void complete(LocalDateTime recheckedAt) {
        if (this.status != ExecutionStatus.STARTED) {
            throw new IllegalStateException("진행 중인 행동만 완료할 수 있습니다.");
        }

        if (recheckedAt == null) {
            throw new IllegalArgumentException("재측정 완료 시각이 필요합니다.");
        }

        this.status = ExecutionStatus.COMPLETED;
        this.recheckedAt = recheckedAt;
    }

}
