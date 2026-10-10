package com.moodandmove.analysis.repository;

import com.moodandmove.analysis.domain.entity.MoodRecheck;
import com.moodandmove.analysis.repository.projection.BestActionProjection;
import com.moodandmove.analysis.repository.projection.CategoryEffectRankProjection;
import com.moodandmove.analysis.repository.projection.CategoryExecutionRankProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoodRecheckRepository extends JpaRepository<MoodRecheck, Long> {

    /*
     * =========================================================
     * 기존 기능
     * 특정 일기의 행동 후 점수 조회
     * =========================================================
     */
    @Query(
            value = """
                    SELECT mr.after_score
                    FROM mood_rechecks mr
                    JOIN action_executions ae
                      ON ae.id = mr.action_execution_id
                    JOIN recommendation_sessions rs
                      ON rs.id = ae.session_id
                    WHERE rs.mood_entry_id = :moodEntryId
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<Integer> findAfterScoreByMoodEntryId(
            @Param("moodEntryId") Long moodEntryId
    );

    /*
     * =========================================================
     * 1. 이번 달 단일 최고 효과 행동
     *
     * 평균이 아니라
     * 실제 재측정 1건 중 delta가 가장 높은 행동 1개
     * =========================================================
     */
    @Query(
            value = """
                    SELECT
                        a.id AS actionId,
                        a.action_name AS actionName,
                        a.category AS category,
                        mr.delta AS delta
                    FROM mood_rechecks mr
                    JOIN action_executions ae
                        ON ae.id = mr.action_execution_id
                    JOIN recommendations r
                        ON r.id = ae.recommendation_id
                    JOIN actions a
                        ON a.id = r.action_id
                    JOIN recommendation_sessions rs
                        ON rs.id = ae.session_id
                    JOIN mood_entries me
                        ON me.id = rs.mood_entry_id
                    WHERE ae.user_id = :userId
                        AND me.entry_date >= :startDate
                        AND me.entry_date < :endDate
                    ORDER BY
                        mr.delta DESC,
                        mr.checked_at DESC
                    LIMIT 1
            """,
        nativeQuery = true
    )
    Optional<BestActionProjection> findBestAction(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
            );

    /*
     * =========================================================
     * 2. 카테고리별 실행 횟수 랭킹
     *
     * WALK 4
     * SOCIAL 3
     * EATING 2
     * =========================================================
     */
    @Query(
            value = """
                SELECT
                    a.category AS category,
                    COUNT(*) AS executionCount
                FROM action_executions ae
                JOIN recommendations r
                    ON r.id = ae.recommendation_id
                JOIN actions a
                    ON a.id = r.action_id
                JOIN recommendation_sessions rs
                    ON rs.id = ae.session_id
                JOIN mood_entries me
                    ON me.id = rs.mood_entry_id
                WHERE ae.user_id = :userId
                    AND ae.status = 'COMPLETED'
                    AND me.entry_date >= :startDate
                    AND me.entry_date < :endDate
                GROUP BY a.category
                ORDER BY
                        executionCount DESC,
                        a.category ASC
                        
            """,
            nativeQuery = true
    )
    List<CategoryExecutionRankProjection>
    findCategoryExecutionRanking(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /*
     * =========================================================
     * 3. 카테고리별 평균 효과 랭킹
     *
     * WALK   14.00
     * EATING 11.00
     * SOCIAL  8.00
     * =========================================================
     */
    @Query(
            value = """
                SELECT
                    a.category AS category,
                    ROUND(AVG(mr.delta), 2) AS averageDelta,
                    COUNT(*) AS sampleCount
                FROM mood_rechecks mr
                JOIN action_executions ae
                    ON ae.id = mr.action_execution_id
                JOIN recommendations r
                    ON r.id = ae.recommendation_id
                JOIN actions a
                    ON a.id = r.action_id
                JOIN recommendation_sessions rs
                    ON rs.id = ae.session_id
                JOIN mood_entries me
                    ON me.id = rs.mood_entry_id
                WHERE ae.user_id = :userId
                    AND me.entry_date >= :startDate
                    AND me.entry_date < :endDate
                GROUP BY a.category
                ORDER BY
                        averageDelta DESC,
                        sampleCount DESC,
                        a.category ASC
            """,
            nativeQuery = true
    )
    List<CategoryEffectRankProjection>
    findCategoryEffectRanking(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
