package com.moodandmove.analysis.domain.entity;

import com.moodandmove.recommendation.domain.entity.ActionExecution;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mood_rechecks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoodRecheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "action_execution_id",
            nullable = false,
            unique = true
    )
    private ActionExecution actionExecution;

    @Column(name = "before_score", nullable = false)
    private Integer beforeScore;

    @Column(name = "after_score")
    private Integer afterScore;

    /*
     * MySQL GENERATED ALWAYS 컬럼.
     * JPA가 INSERT/UPDATE 하지 않는다.
     */
    @Column(
            name = "delta",
            insertable = false,
            updatable = false
    )
    private Integer delta;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    // 행동 선택시 재측정 전 점수만 저장할 객체 생성
    public static MoodRecheck prepare(
            ActionExecution actionExecution,
            Integer beforeScore){
        MoodRecheck recheck = new MoodRecheck();

        recheck.actionExecution = actionExecution;
        recheck.beforeScore = beforeScore;
        return recheck;
    }

    // 재측정 완료 시 점수와 완료 시각
    public void complete(
            Integer afterScore,
            LocalDateTime checkedAt){
        if(this.afterScore != null || this.checkedAt != null){
            throw new IllegalStateException("이미 재측정이 완료되었습니다.");
        }
        if (afterScore == null || afterScore < 1 || afterScore > 60){
            throw new IllegalStateException("감정 점수는 1~60점 이어야 합니다");
        }
        if (checkedAt == null) {
            throw new IllegalStateException("재측정 완료 시간이 필요합니다.");
        }
            this.afterScore = afterScore;
            this.checkedAt = checkedAt;
            this.delta = afterScore - this.beforeScore;

    }
}