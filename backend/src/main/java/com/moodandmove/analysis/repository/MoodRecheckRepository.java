package com.moodandmove.analysis.repository;

import com.moodandmove.analysis.domain.entity.MoodRecheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MoodRecheckRepository extends JpaRepository<MoodRecheck, Long> {

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
}
