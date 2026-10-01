package com.moodandmove.analysis.domain.entity;

import com.moodandmove.recommendation.domain.entity.ActionExecution;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(name = "after_score", nullable = false)
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

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;
}