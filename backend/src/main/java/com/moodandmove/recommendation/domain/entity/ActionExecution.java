
package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.recommendation.domain.type.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    // 실행 완료 시각
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 기분 재측정 완료 시각
    @Column(name = "rechecked_at")
    private LocalDateTime recheckedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
