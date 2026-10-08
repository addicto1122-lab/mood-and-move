-- MoodAndMove | Recommendation system schema migration
-- Save as: V<next_version>__restructure_recommendation_system.sql
-- Database: MySQL 8.0.16+ (CHECK constraints enforced)
-- DEVELOPMENT-ONLY, DATA-DESTRUCTIVE MIGRATION.
-- Based on supplied FKs.csv, 1FKs.csv, 2FKs.csv and 3FKs.csv.
-- IMPORTANT:
--   * Run against the original schema exactly once through Flyway.
--   * Confirm users.id is BIGINT UNSIGNED (as assumed by existing user_id FKs).
--   * Application entities/repositories and enum mappings MUST be deployed together.
--   * Back up the DB first. MySQL DDL is not wholly transactional.
--   * No standalone category lookup table: category stores ActionCategory enum names.

-- ---------------------------------------------------------------------------
-- 1. Clear old recommendation/action-dependent data (child tables first).
--    Users, mood_entries, emotions, hobbies, and other core records remain.
-- ---------------------------------------------------------------------------
DELETE FROM recommendation_places;
DELETE FROM mood_rechecks;
DELETE FROM action_executions;
DELETE FROM recommendations;
DELETE FROM recommendation_sessions;
DELETE FROM action_hobbies;
DELETE FROM user_action_dislikes;
DELETE FROM user_action_stats;
DELETE FROM actions;

-- ---------------------------------------------------------------------------
-- 2. Drop ONLY the obsolete CHECK constraints verified in 2FKs.csv.
--    Keep actions' environment/social/activity_style CHECK constraints.
--    Keep recommendation_sessions.chk_recommendation_type.
-- ---------------------------------------------------------------------------
ALTER TABLE action_executions
DROP CHECK chk_action_execution_selected,
    DROP CHECK chk_action_execution_status,
    DROP CHECK chk_action_execution_type;

ALTER TABLE recommendation_sessions
DROP CHECK chk_recommendation_selection_status,
    DROP CHECK chk_recommendation_time_bucket;

-- ---------------------------------------------------------------------------
-- 3. Remove old execution FKs before changing their participating columns.
--    Keep fk_action_executions_session and fk_mood_rechecks_execution.
--    The referenced recommendations(action_id, id) FK is being retired.
-- ---------------------------------------------------------------------------
ALTER TABLE action_executions
DROP FOREIGN KEY fk_action_execution_recommended_action,
    DROP FOREIGN KEY fk_action_execution_selected_recommendation,
    DROP FOREIGN KEY fk_action_executions_action;

-- ---------------------------------------------------------------------------
-- 4. Actions: one reusable row per action_name, across ALL sessions/users.
--    Repeated LLM action names must REUSE the existing actions.id.
-- ---------------------------------------------------------------------------
ALTER TABLE actions
DROP COLUMN action_code,
    CHANGE COLUMN name action_name VARCHAR(100) NOT NULL,
    DROP COLUMN active,
    DROP COLUMN updated_at,
    ADD CONSTRAINT uq_actions_action_name UNIQUE (action_name),
    ADD CONSTRAINT chk_actions_category CHECK (
        category IN (
            'WALK', 'EXERCISE', 'STRETCHING', 'MEDITATION', 'SLEEP',
            'MUSIC', 'READING', 'ENTERTAINMENT', 'SOCIAL', 'OUTDOOR',
            'EATING', 'SELF_CARE', 'CLEANING'
        )
    ),
    ADD CONSTRAINT chk_actions_duration CHECK (duration_minutes > 0),
    ADD CONSTRAINT chk_actions_location_required CHECK (location_required IN (0, 1));

-- Existing actions CHECKs (kept):
-- chk_actions_environment_type: INDOOR / OUTDOOR / ANY
-- chk_actions_social_type:       ALONE / SOCIAL / ANY
-- chk_actions_activity_style:    ACTIVE / CALM / ANY

-- ---------------------------------------------------------------------------
-- 5. Recommendation sessions: one per mood entry.
--    Existing UNIQUE(mood_entry_id), FK(mood_entry_id), and
--    chk_recommendation_type are kept.
-- ---------------------------------------------------------------------------
ALTER TABLE recommendation_sessions
    ADD COLUMN user_id BIGINT UNSIGNED NOT NULL AFTER id,
    MODIFY COLUMN time_bucket VARCHAR(20) NOT NULL,
DROP COLUMN available_minutes,
    DROP COLUMN weather_condition,
    DROP COLUMN temperature_celsius,
    DROP COLUMN precipitation_mm,
    DROP COLUMN weather_observed_at,
    DROP COLUMN selection_status,
    ADD CONSTRAINT fk_recommendation_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    ADD CONSTRAINT chk_recommendation_sessions_time_bucket CHECK (
        time_bucket IN ('MORNING', 'AFTERNOON', 'EVENING', 'NIGHT')
    );

-- ---------------------------------------------------------------------------
-- 6. Recommendations: three candidates per session (enforced by service).
--    Existing FK(session_id) and FK(action_id) are retained.
--    Existing UNIQUE(session_id, rank_no), UNIQUE(session_id, action_id),
--    and UNIQUE(session_id, id) are retained (verified in 3FKs.csv).
--    Newly added VIRTUAL generated-column UNIQUE enforces <= 1 SELECTED.
--    VIRTUAL avoids STORED generated-column / FK CASCADE restrictions.
-- ---------------------------------------------------------------------------
ALTER TABLE recommendations
DROP COLUMN score,
    DROP COLUMN reason_code,
    MODIFY COLUMN reason TEXT NOT NULL,
    ADD COLUMN recheck_completed TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN selected_at DATETIME NULL DEFAULT NULL,
    ADD COLUMN selected_session_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN status = 'SELECTED' THEN session_id ELSE NULL END
        ) VIRTUAL,
    ADD CONSTRAINT uq_recommendations_one_selected UNIQUE (selected_session_id),
    ADD CONSTRAINT chk_recommendations_rank CHECK (rank_no BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_recommendations_status CHECK (
        status IN ('PENDING', 'SELECTED', 'UNSELECTED', 'SKIPPED')
    ),
    ADD CONSTRAINT chk_recommendations_recheck CHECK (
        recheck_completed IN (0, 1)
        AND (recheck_completed = 0 OR status = 'SELECTED')
    ),
    ADD CONSTRAINT chk_recommendations_selected_at CHECK (
        (status = 'SELECTED' AND selected_at IS NOT NULL)
        OR (status <> 'SELECTED' AND selected_at IS NULL)
    );

-- ---------------------------------------------------------------------------
-- 7. Executions: one selected recommendation executed per session.
--    Existing UNIQUE(session_id) and FK(session_id) are retained.
--    mood_rechecks.action_execution_id already has a UNIQUE FK to this table.
--    We deliberately do NOT add a reverse mood_rechecks_id FK.
-- ---------------------------------------------------------------------------
ALTER TABLE action_executions
    CHANGE COLUMN selected_recommendation_id recommendation_id BIGINT UNSIGNED NOT NULL,
DROP COLUMN performed_action_id,
    DROP COLUMN execution_type,
    DROP COLUMN recheck_available_at,
    DROP COLUMN recheck_expires_at,
    ADD COLUMN user_id BIGINT UNSIGNED NOT NULL AFTER id,
    ADD COLUMN rechecked_at DATETIME NULL DEFAULT NULL,
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'STARTED',
    MODIFY COLUMN started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT uq_action_executions_recommendation UNIQUE (recommendation_id),
    ADD CONSTRAINT fk_action_executions_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_action_executions_session_recommendation
        FOREIGN KEY (session_id, recommendation_id)
        REFERENCES recommendations(session_id, id),
    ADD CONSTRAINT chk_action_executions_status CHECK (
        status IN ('STARTED', 'COMPLETED', 'CANCELED')
    );

-- ---------------------------------------------------------------------------
-- 8. Category scores: ONE PersonalScore per (user, category).
--    avg_delta is NULL until the first valid recheck sample is collected.
-- ---------------------------------------------------------------------------
CREATE TABLE action_category_scores (
                                        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                        user_id BIGINT UNSIGNED NOT NULL,
                                        category VARCHAR(30) NOT NULL,
                                        personal_score DECIMAL(5,2) NOT NULL DEFAULT 50.00,
                                        avg_delta DECIMAL(6,2) NULL DEFAULT NULL,
                                        positive_count INT UNSIGNED NOT NULL DEFAULT 0,
                                        sample_count INT UNSIGNED NOT NULL DEFAULT 0,
                                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,

                                        PRIMARY KEY (id),
                                        CONSTRAINT uq_action_category_scores_user_category
                                            UNIQUE (user_id, category),
                                        CONSTRAINT fk_action_category_scores_user
                                            FOREIGN KEY (user_id) REFERENCES users(id),
                                        CONSTRAINT chk_action_category_scores_category CHECK (
                                            category IN (
                                                         'WALK', 'EXERCISE', 'STRETCHING', 'MEDITATION', 'SLEEP',
                                                         'MUSIC', 'READING', 'ENTERTAINMENT', 'SOCIAL', 'OUTDOOR',
                                                         'EATING', 'SELF_CARE', 'CLEANING'
                                                )
                                            ),
                                        CONSTRAINT chk_action_category_scores_score
                                            CHECK (personal_score BETWEEN 0 AND 100),
                                        CONSTRAINT chk_action_category_scores_delta
                                            CHECK (avg_delta BETWEEN -59 AND 59),
                                        CONSTRAINT chk_action_category_scores_counts
                                            CHECK (positive_count <= sample_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- SERVICE-SIDE INVARIANTS (not fully expressible with these constraints):
--  * Generate/validate exactly 3 candidates, ranks 1-3, in ONE transaction.
--  * Normalize action_name and reuse actions.id on duplicate (handle races).
--  * Verify recommendation_sessions.user_id = mood_entries.user_id.
--  * Verify action_executions.user_id = session.user_id.
--  * Only execute the recommendation with status SELECTED.
--  * Set recommendations.recheck_completed=1 only after a valid mood_recheck.
--  * Increment category sample_count exactly ONCE per completed recheck.
--  * Keep user_action_stats / user_action_dislikes / action_hobbies code in sync.
--  * Old mood_entries with recommendation_status='REQUESTED' may need reset
--    separately in DEV if their sessions were deleted above.
-- ---------------------------------------------------------------------------
