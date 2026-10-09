package com.moodandmove.analysis.repository;

import com.moodandmove.analysis.domain.entity.MoodRecheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface MoodRecheckRepository
        extends JpaRepository<MoodRecheck, Long> {

    // 일기에 연결된 재측정 완료 점수 조회
    @Query(value = """
            SELECT mr.after_score
            FROM mood_rechecks mr
            JOIN action_executions ae
              ON ae.id = mr.action_execution_id
            JOIN recommendation_sessions rs
              ON rs.id = ae.session_id
            WHERE rs.mood_entry_id = :moodEntryId
              AND ae.status = 'COMPLETED'
              AND mr.after_score IS NOT NULL
              AND mr.checked_at IS NOT NULL
            LIMIT 1
            """, nativeQuery = true)
    Optional<Integer> findAfterScoreByMoodEntryId(
            @Param("moodEntryId") Long moodEntryId
    );

    // 실행 기록에 연결된 재측정 행 조회
    Optional<MoodRecheck> findByActionExecution_Id(Long executionId);

    // 사용자·카테고리별 재측정 완료 기록 집계(샘플 수, 긍정 수, 델타평균)
    @Query(value = """
            SELECT COUNT(*) AS sampleCount,
                   COALESCE(SUM(mr.delta > 0), 0) AS positiveCount,
                   AVG(mr.delta) AS avgDelta
            FROM mood_rechecks mr
            JOIN action_executions ae
              ON ae.id = mr.action_execution_id
            JOIN recommendations r
              ON r.id = ae.recommendation_id
            JOIN actions a
              ON a.id = r.action_id
            WHERE ae.user_id = :userId
              AND a.category = :category
              AND ae.status = 'COMPLETED'
              AND r.recheck_completed = 1
              AND mr.after_score IS NOT NULL
              AND mr.checked_at IS NOT NULL
            """, nativeQuery = true)
    CategorySummary summarizeCategory(
            @Param("userId") Long userId,
            @Param("category") String category
    );

    interface CategorySummary {
        long getSampleCount();
        long getPositiveCount();
        BigDecimal getAvgDelta();
    }
}