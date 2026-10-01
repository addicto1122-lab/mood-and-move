package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.recommendation.domain.type.ExecutionStatus;
import com.moodandmove.recommendation.domain.type.ExecutionType;
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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private RecommendationSession session;

    /*
     * DB에서는
     * (session_id, selected_recommendation_id)
     * Composite FK로 동일 Session 여부를 추가 검증한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_recommendation_id")
    private Recommendation selectedRecommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_action_id", nullable = false)
    private Action performedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type", nullable = false, length = 20)
    private ExecutionType executionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "recheck_available_at")
    private LocalDateTime recheckAvailableAt;

    @Column(name = "recheck_expires_at")
    private LocalDateTime recheckExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}