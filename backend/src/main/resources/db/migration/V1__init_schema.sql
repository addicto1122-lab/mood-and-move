-- =========================================================
-- Mood&Move
-- V1__init_schema.sql
-- MySQL 8.x
-- =========================================================


-- =========================================================
-- 1. USERS
-- 담당: 이래원
-- =========================================================
CREATE TABLE users (
                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       nickname VARCHAR(50) NOT NULL,

                       onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,

                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT uq_users_email
                           UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 2. REFRESH_TOKENS
-- 담당: 이래원
-- =========================================================
CREATE TABLE refresh_tokens (
                                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                user_id BIGINT UNSIGNED NOT NULL,

                                token_hash VARCHAR(255) NOT NULL,

                                expires_at DATETIME NOT NULL,
                                revoked_at DATETIME NULL,

                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT uq_refresh_tokens_token_hash
                                    UNIQUE (token_hash),

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                INDEX idx_refresh_tokens_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 3. USER_PREFERENCES
-- 담당: 이래원
-- 온보딩 / Cold Start 기준
-- =========================================================
CREATE TABLE user_preferences (
                                  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                  user_id BIGINT UNSIGNED NOT NULL,

    -- ACTIVE / CALM / ANY
                                  rest_style VARCHAR(20) NOT NULL DEFAULT 'ANY',

    -- INDOOR / OUTDOOR / ANY
                                  activity_environment VARCHAR(20) NOT NULL DEFAULT 'ANY',

    -- ALONE / SOCIAL / ANY
                                  social_preference VARCHAR(20) NOT NULL DEFAULT 'ANY',

                                  default_available_minutes SMALLINT UNSIGNED,

                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  CONSTRAINT uq_user_preferences_user_id
                                      UNIQUE (user_id),

                                  CONSTRAINT chk_user_preferences_rest_style
                                      CHECK (
                                          rest_style IN (
                                                         'ACTIVE',
                                                         'CALM',
                                                         'ANY'
                                              )
                                          ),

                                  CONSTRAINT chk_user_preferences_environment
                                      CHECK (
                                          activity_environment IN (
                                                                   'INDOOR',
                                                                   'OUTDOOR',
                                                                   'ANY'
                                              )
                                          ),

                                  CONSTRAINT chk_user_preferences_social
                                      CHECK (
                                          social_preference IN (
                                                                'ALONE',
                                                                'SOCIAL',
                                                                'ANY'
                                              )
                                          ),

                                  CONSTRAINT fk_user_preferences_user
                                      FOREIGN KEY (user_id)
                                          REFERENCES users(id)
                                          ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 4. HOBBIES
-- 담당: 이래원
-- 취미 Master
-- =========================================================
CREATE TABLE hobbies (
                         id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                         name VARCHAR(50) NOT NULL,
                         category VARCHAR(30),

                         active BOOLEAN NOT NULL DEFAULT TRUE,

                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uq_hobbies_name
                             UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 5. USER_HOBBIES
-- 담당: 이래원
-- 사용자 <-> 취미 N:M
-- =========================================================
CREATE TABLE user_hobbies (
                              user_id BIGINT UNSIGNED NOT NULL,
                              hobby_id BIGINT UNSIGNED NOT NULL,

                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              PRIMARY KEY (user_id, hobby_id),

                              CONSTRAINT fk_user_hobbies_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_user_hobbies_hobby
                                  FOREIGN KEY (hobby_id)
                                      REFERENCES hobbies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 6. EMOTIONS
-- 담당: 이래원
-- 감정 종류 Master
-- =========================================================
CREATE TABLE emotions (
                          emotion_code VARCHAR(30) PRIMARY KEY,

                          name VARCHAR(50) NOT NULL,

                          emoji VARCHAR(10),

                          active BOOLEAN NOT NULL DEFAULT TRUE,

                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT uq_emotions_name
                              UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 7. ACTIVITY_TAGS
-- 담당: 이래원
-- 현재 활동 Master
-- =========================================================
CREATE TABLE activity_tags (
                               id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                               name VARCHAR(50) NOT NULL,
                               category VARCHAR(30),

                               active BOOLEAN NOT NULL DEFAULT TRUE,

                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT uq_activity_tags_name
                                   UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 8. MOOD_ENTRIES
-- 담당: 이래원
--
-- 정책
-- - 사용자당 하루 1회
-- - 사용자 수정 불가
-- - Soft Delete 가능
-- - 삭제 후 같은 날짜 재작성 불가
-- =========================================================
CREATE TABLE mood_entries (
                              id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                              user_id BIGINT UNSIGNED NOT NULL,

    -- 하루 한 번 정책 기준
                              entry_date DATE NOT NULL,

                              emotion_code VARCHAR(30) NOT NULL,

    -- 현재 기분의 긍정/부정 정도 1~5
                              mood_score TINYINT UNSIGNED NOT NULL,

    -- 선택한 감정의 강도 1~5
                              intensity TINYINT UNSIGNED NOT NULL,

                              diary_content TEXT,

    -- AVAILABLE / REQUESTED / DECLINED / EXPIRED / DELETED
                              recommendation_status VARCHAR(20)
                                  NOT NULL DEFAULT 'AVAILABLE',

    -- 추천 요청 가능 시간을 둘 경우 사용
                              recommendation_eligible_until DATETIME NULL,

                              recorded_at DATETIME NOT NULL,

    -- Soft Delete
                              deleted_at DATETIME NULL,

                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT uq_mood_entries_user_date
                                  UNIQUE (user_id, entry_date),

                              CONSTRAINT chk_mood_entries_score
                                  CHECK (
                                      mood_score BETWEEN 1 AND 5
                                      ),

                              CONSTRAINT chk_mood_entries_intensity
                                  CHECK (
                                      intensity BETWEEN 1 AND 5
                                      ),

                              CONSTRAINT chk_mood_entries_recommendation_status
                                  CHECK (
                                      recommendation_status IN (
                                                                'AVAILABLE',
                                                                'REQUESTED',
                                                                'DECLINED',
                                                                'EXPIRED',
                                                                'DELETED'
                                          )
                                      ),

                              CONSTRAINT fk_mood_entries_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_mood_entries_emotion
                                  FOREIGN KEY (emotion_code)
                                      REFERENCES emotions(emotion_code),

                              INDEX idx_mood_entries_user_entry_date
                                  (user_id, entry_date),

                              INDEX idx_mood_entries_deleted_at
                                  (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 9. MOOD_ENTRY_ACTIVITIES
-- 담당: 이래원
-- 감정일기 <-> 현재 활동 N:M
-- =========================================================
CREATE TABLE mood_entry_activities (
                                       mood_entry_id BIGINT UNSIGNED NOT NULL,

                                       activity_tag_id BIGINT UNSIGNED NOT NULL,

                                       PRIMARY KEY (
                                                    mood_entry_id,
                                                    activity_tag_id
                                           ),

                                       CONSTRAINT fk_mood_entry_activities_mood
                                           FOREIGN KEY (mood_entry_id)
                                               REFERENCES mood_entries(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_mood_entry_activities_tag
                                           FOREIGN KEY (activity_tag_id)
                                               REFERENCES activity_tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 10. ACTIONS
-- 담당: 장준호
-- 추천 가능한 행동 Master
-- =========================================================
CREATE TABLE actions (
                         id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                         action_code VARCHAR(50) NOT NULL,

                         name VARCHAR(100) NOT NULL,

                         category VARCHAR(30) NOT NULL,

                         duration_minutes SMALLINT UNSIGNED NOT NULL,

    -- INDOOR / OUTDOOR / ANY
                         environment_type VARCHAR(20)
                                                 NOT NULL DEFAULT 'ANY',

    -- ALONE / SOCIAL / ANY
                         social_type VARCHAR(20)
                                                 NOT NULL DEFAULT 'ANY',

    -- ACTIVE / CALM / ANY
                         activity_style VARCHAR(20)
                                                 NOT NULL DEFAULT 'ANY',

                         location_required BOOLEAN
                                                 NOT NULL DEFAULT FALSE,

                         place_category VARCHAR(50),

                         active BOOLEAN NOT NULL DEFAULT TRUE,

                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT uq_actions_action_code
                             UNIQUE (action_code),

                         CONSTRAINT chk_actions_environment_type
                             CHECK (
                                 environment_type IN (
                                                      'INDOOR',
                                                      'OUTDOOR',
                                                      'ANY'
                                     )
                                 ),

                         CONSTRAINT chk_actions_social_type
                             CHECK (
                                 social_type IN (
                                                 'ALONE',
                                                 'SOCIAL',
                                                 'ANY'
                                     )
                                 ),

                         CONSTRAINT chk_actions_activity_style
                             CHECK (
                                 activity_style IN (
                                                    'ACTIVE',
                                                    'CALM',
                                                    'ANY'
                                     )
                                 )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 11. ACTION_HOBBIES
-- 담당: 장준호
-- 행동 <-> 취미 N:M
-- =========================================================
CREATE TABLE action_hobbies (
                                action_id BIGINT UNSIGNED NOT NULL,

                                hobby_id BIGINT UNSIGNED NOT NULL,

                                PRIMARY KEY (
                                             action_id,
                                             hobby_id
                                    ),

                                CONSTRAINT fk_action_hobbies_action
                                    FOREIGN KEY (action_id)
                                        REFERENCES actions(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_action_hobbies_hobby
                                    FOREIGN KEY (hobby_id)
                                        REFERENCES hobbies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 12. USER_ACTION_DISLIKES
-- 담당: 이래원
-- 추천 엔진에서 제외할 비선호 행동
-- =========================================================
CREATE TABLE user_action_dislikes (
                                      user_id BIGINT UNSIGNED NOT NULL,

                                      action_id BIGINT UNSIGNED NOT NULL,

                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      PRIMARY KEY (
                                                   user_id,
                                                   action_id
                                          ),

                                      CONSTRAINT fk_user_action_dislikes_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_user_action_dislikes_action
                                          FOREIGN KEY (action_id)
                                              REFERENCES actions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 13. RECOMMENDATION_SESSIONS
-- 담당: 장준호
--
-- 한 감정일기당 최대 한 번의 추천 요청
-- 날씨 등 추천 당시 공통 Context 저장
-- =========================================================
CREATE TABLE recommendation_sessions (
                                         id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                         mood_entry_id BIGINT UNSIGNED NOT NULL,

    -- COLD_START / HYBRID / PERSONALIZED
                                         recommendation_type VARCHAR(20) NOT NULL,

    -- MORNING / AFTERNOON / EVENING / NIGHT
                                         time_bucket VARCHAR(20),

                                         available_minutes SMALLINT UNSIGNED,

                                         weather_condition VARCHAR(30),

                                         temperature_celsius DECIMAL(4,1),

                                         precipitation_mm DECIMAL(6,2),

                                         weather_observed_at DATETIME,

    -- GENERATED / SELECTED / SKIPPED
                                         selection_status VARCHAR(20)
                                                                         NOT NULL DEFAULT 'GENERATED',

                                         requested_at DATETIME
                                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 한 일기당 Session 최대 한 개
                                         CONSTRAINT uq_recommendation_sessions_mood_entry
                                             UNIQUE (mood_entry_id),

                                         CONSTRAINT fk_recommendation_sessions_mood
                                             FOREIGN KEY (mood_entry_id)
                                                 REFERENCES mood_entries(id)
                                                 ON DELETE CASCADE,

                                         CONSTRAINT chk_recommendation_type
                                             CHECK (
                                                 recommendation_type IN (
                                                                         'COLD_START',
                                                                         'HYBRID',
                                                                         'PERSONALIZED'
                                                     )
                                                 ),

                                         CONSTRAINT chk_recommendation_time_bucket
                                             CHECK (
                                                 time_bucket IS NULL
                                                     OR time_bucket IN (
                                                                        'MORNING',
                                                                        'AFTERNOON',
                                                                        'EVENING',
                                                                        'NIGHT'
                                                     )
                                                 ),

                                         CONSTRAINT chk_recommendation_selection_status
                                             CHECK (
                                                 selection_status IN (
                                                                      'GENERATED',
                                                                      'SELECTED',
                                                                      'SKIPPED'
                                                     )
                                                 )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 14. RECOMMENDATIONS
-- 담당: 장준호
-- Rule Engine 실제 추천 결과
-- =========================================================
CREATE TABLE recommendations (
                                 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                 session_id BIGINT UNSIGNED NOT NULL,

                                 action_id BIGINT UNSIGNED NOT NULL,

                                 score DECIMAL(6,2) NOT NULL,

                                 rank_no SMALLINT UNSIGNED NOT NULL,

                                 reason_code VARCHAR(100),

                                 created_at DATETIME
                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_recommendations_session
                                     FOREIGN KEY (session_id)
                                         REFERENCES recommendation_sessions(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_recommendations_action
                                     FOREIGN KEY (action_id)
                                         REFERENCES actions(id),

    -- 한 Session 내 동일 순위 금지
                                 CONSTRAINT uq_recommendations_session_rank
                                     UNIQUE (session_id, rank_no),

    -- 같은 행동 중복 추천 금지
                                 CONSTRAINT uq_recommendations_session_action
                                     UNIQUE (session_id, action_id),

    -- ACTION_EXECUTIONS Composite FK 지원
                                 CONSTRAINT uq_recommendations_session_id
                                     UNIQUE (session_id, id),

                                 INDEX idx_recommendations_action_id
                                     (action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 15. RECOMMENDATION_PLACES
-- 담당: 장준호
--
-- MVP:
-- Recommendation 1개당 실제 장소 최대 1개
-- 장소 필요 없는 행동은 Row 없음
-- =========================================================
CREATE TABLE recommendation_places (
                                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                       recommendation_id BIGINT UNSIGNED NOT NULL,

                                       provider VARCHAR(30),

                                       external_place_id VARCHAR(100),

                                       place_name VARCHAR(150) NOT NULL,

                                       place_category VARCHAR(50),

                                       address VARCHAR(255),

                                       latitude DECIMAL(10,7),

                                       longitude DECIMAL(10,7),

                                       distance_meters INT UNSIGNED,

                                       created_at DATETIME
                                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT uq_recommendation_places_recommendation
                                           UNIQUE (recommendation_id),

                                       CONSTRAINT fk_recommendation_places_recommendation
                                           FOREIGN KEY (recommendation_id)
                                               REFERENCES recommendations(id)
                                               ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 16. ACTION_EXECUTIONS
-- 담당: 장준호
--
-- Session 하나에서 행동 하나만 수행
-- 다른 Session Recommendation 선택 방지
-- =========================================================
CREATE TABLE action_executions (
                                   id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                   session_id BIGINT UNSIGNED NOT NULL,

    -- 추천된 행동 선택 시 Recommendation ID
    -- ALTERNATIVE이면 NULL
                                   selected_recommendation_id BIGINT UNSIGNED NULL,

    -- 실제로 수행한 ACTION
                                   performed_action_id BIGINT UNSIGNED NOT NULL,

    -- RECOMMENDED / ALTERNATIVE
                                   execution_type VARCHAR(20) NOT NULL,

    -- STARTED / COMPLETED / CANCELLED
                                   status VARCHAR(20) NOT NULL,

                                   started_at DATETIME NULL,

                                   completed_at DATETIME NULL,

    -- 완료 후 재측정 가능 구간
                                   recheck_available_at DATETIME NULL,

                                   recheck_expires_at DATETIME NULL,

                                   created_at DATETIME
                                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   updated_at DATETIME
                                                              NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

    -- Session 하나에 실제 수행 행동 하나
                                   CONSTRAINT uq_action_executions_session
                                       UNIQUE (session_id),

                                   CONSTRAINT fk_action_executions_session
                                       FOREIGN KEY (session_id)
                                           REFERENCES recommendation_sessions(id)
                                           ON DELETE CASCADE,

    -- 선택한 Recommendation이
    -- 해당 Session 소속인지 DB에서 검증
                                   CONSTRAINT fk_action_execution_selected_recommendation
                                       FOREIGN KEY (
                                                    session_id,
                                                    selected_recommendation_id
                                           )
                                           REFERENCES recommendations (
                                                                       session_id,
                                                                       id
                                               ),

                                   CONSTRAINT fk_action_executions_action
                                       FOREIGN KEY (performed_action_id)
                                           REFERENCES actions(id),

                                   CONSTRAINT chk_action_execution_type
                                       CHECK (
                                           execution_type IN (
                                                              'RECOMMENDED',
                                                              'ALTERNATIVE'
                                               )
                                           ),

                                   CONSTRAINT chk_action_execution_selected
                                       CHECK (
                                           (
                                               execution_type = 'RECOMMENDED'
                                                   AND selected_recommendation_id IS NOT NULL
                                               )
                                               OR
                                           (
                                               execution_type = 'ALTERNATIVE'
                                                   AND selected_recommendation_id IS NULL
                                               )
                                           ),

                                   CONSTRAINT chk_action_execution_status
                                       CHECK (
                                           status IN (
                                                      'STARTED',
                                                      'COMPLETED',
                                                      'CANCELLED'
                                               )
                                           ),

                                   INDEX idx_action_executions_action
                                       (performed_action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 17. MOOD_RECHECKS
-- 담당: 장준호
-- 행동 완료 후 감정 재측정
-- =========================================================
CREATE TABLE mood_rechecks (
                               id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                               action_execution_id BIGINT UNSIGNED NOT NULL,

                               before_score TINYINT UNSIGNED NOT NULL,

                               after_score TINYINT UNSIGNED NOT NULL,

                               delta TINYINT
                                   GENERATED ALWAYS AS
                                       (after_score - before_score) STORED,

                               checked_at DATETIME
                                   NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT uq_mood_rechecks_execution
                                   UNIQUE (action_execution_id),

                               CONSTRAINT chk_mood_rechecks_before
                                   CHECK (
                                       before_score BETWEEN 1 AND 5
                                       ),

                               CONSTRAINT chk_mood_rechecks_after
                                   CHECK (
                                       after_score BETWEEN 1 AND 5
                                       ),

                               CONSTRAINT fk_mood_rechecks_execution
                                   FOREIGN KEY (action_execution_id)
                                       REFERENCES action_executions(id)
                                       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 18. USER_ACTION_STATS
-- 담당: 백기완
-- 사용자별 행동 효과 Summary
-- =========================================================
CREATE TABLE user_action_stats (
                                   id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                   user_id BIGINT UNSIGNED NOT NULL,

                                   action_id BIGINT UNSIGNED NOT NULL,

                                   recommendation_count INT UNSIGNED
        NOT NULL DEFAULT 0,

                                   execution_count INT UNSIGNED
        NOT NULL DEFAULT 0,

                                   completed_count INT UNSIGNED
        NOT NULL DEFAULT 0,

                                   recheck_count INT UNSIGNED
        NOT NULL DEFAULT 0,

                                   positive_count INT UNSIGNED
        NOT NULL DEFAULT 0,

                                   avg_delta DECIMAL(5,2)
                                       NOT NULL DEFAULT 0,

                                   calculated_at DATETIME
                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uq_user_action_stats
                                       UNIQUE (user_id, action_id),

                                   CONSTRAINT fk_user_action_stats_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES users(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_user_action_stats_action
                                       FOREIGN KEY (action_id)
                                           REFERENCES actions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- =========================================================
-- 19. PERSONAL_RULES
-- 담당: 백기완
--
-- 언제 / 어떤 상태에서 / 어떤 행동이
-- 효과가 있었는지 저장
-- =========================================================
CREATE TABLE personal_rules (
                                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                user_id BIGINT UNSIGNED NOT NULL,

                                action_id BIGINT UNSIGNED NOT NULL,

                                mood_min TINYINT UNSIGNED,

                                mood_max TINYINT UNSIGNED,

                                time_bucket VARCHAR(20),

                                activity_tag_id BIGINT UNSIGNED NULL,

                                sample_count INT UNSIGNED
        NOT NULL DEFAULT 0,

                                positive_count INT UNSIGNED
        NOT NULL DEFAULT 0,

                                positive_rate DECIMAL(5,2),

                                avg_delta DECIMAL(5,2),

    -- LOW / MEDIUM / HIGH
                                confidence_level VARCHAR(20),

                                active BOOLEAN NOT NULL DEFAULT TRUE,

                                calculated_at DATETIME
                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_at DATETIME
                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                updated_at DATETIME
                                               NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

                                CONSTRAINT fk_personal_rules_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_personal_rules_action
                                    FOREIGN KEY (action_id)
                                        REFERENCES actions(id),

                                CONSTRAINT fk_personal_rules_activity_tag
                                    FOREIGN KEY (activity_tag_id)
                                        REFERENCES activity_tags(id)
                                        ON DELETE SET NULL,

                                CONSTRAINT chk_personal_rules_mood_min
                                    CHECK (
                                        mood_min IS NULL
                                            OR mood_min BETWEEN 1 AND 5
                                        ),

                                CONSTRAINT chk_personal_rules_mood_max
                                    CHECK (
                                        mood_max IS NULL
                                            OR mood_max BETWEEN 1 AND 5
                                        ),

                                CONSTRAINT chk_personal_rules_confidence
                                    CHECK (
                                        confidence_level IS NULL
                                            OR confidence_level IN (
                                                                    'LOW',
                                                                    'MEDIUM',
                                                                    'HIGH'
                                            )
                                        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;