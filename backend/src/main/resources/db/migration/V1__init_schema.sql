-- ============================================================

-- Mood&Move | V1__init_schema.sql (consolidated from V1-V12)

-- MySQL 8.0.16+ / InnoDB / UTF-8

-- Fresh local DBs only: all developers must DROP/CREATE their DB

-- before changing shared Flyway migration history.

-- All existing final V12 columns, constraints, and indexes kept;

-- FK-only fix: 3 direct user FKs + 1 dependent recommendation FK

-- use ON DELETE CASCADE for account deletion.

-- No DELETE/INSERT/data operations in this file.

-- ============================================================


-- [01/23] users | V1 + V3(age_group/gender) + V7(local_login_enabled)
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    local_login_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    nickname VARCHAR(50) NOT NULL,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    token_version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    age_group VARCHAR(20) NULL,
    gender VARCHAR(20) NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [02/23] refresh_tokens | V1
CREATE TABLE refresh_tokens (
                                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                user_id BIGINT UNSIGNED NOT NULL,

    -- 사용자 로그인 세대
                                token_version INT UNSIGNED NOT NULL,

    -- 실제 Refresh Token 원문 대신 Hash 저장
                                token_hash VARCHAR(255) NOT NULL,

                                expires_at DATETIME NOT NULL,

    -- 로그아웃 / 재로그인 등으로 폐기된 시점
                                revoked_at DATETIME NULL,

                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT uq_refresh_tokens_token_hash
                                    UNIQUE (token_hash),

    -- 한 사용자가 같은 version을 중복 발급하지 않도록
                                CONSTRAINT uq_refresh_tokens_user_version
                                    UNIQUE (user_id, token_version),

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                INDEX idx_refresh_tokens_user_id
                                    (user_id),

                                INDEX idx_refresh_tokens_user_version
                                    (user_id, token_version),

                                INDEX idx_refresh_tokens_expires_at
                                    (expires_at)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [03/23] user_preferences | V1 + V5(default region)
CREATE TABLE user_preferences (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    activity_style VARCHAR(20) NOT NULL DEFAULT 'ANY',
    activity_environment VARCHAR(20) NOT NULL DEFAULT 'ANY',
    social_preference VARCHAR(20) NOT NULL DEFAULT 'ANY',
    default_available_minutes INT UNSIGNED,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    default_region_name VARCHAR(100) NULL,
    default_region_code VARCHAR(30) NULL,
    default_region_latitude DECIMAL(10, 7) NULL,
    default_region_longitude DECIMAL(10, 7) NULL,
    CONSTRAINT uq_user_preferences_user_id UNIQUE (user_id),
    CONSTRAINT chk_user_preferences_activity_style
        CHECK (activity_style IN ('ACTIVE', 'CALM', 'ANY')),
    CONSTRAINT chk_user_preferences_environment
        CHECK (activity_environment IN ('INDOOR', 'OUTDOOR', 'ANY')),
    CONSTRAINT chk_user_preferences_social
        CHECK (social_preference IN ('ALONE', 'SOCIAL', 'ANY')),
    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [04/23] hobbies | V1
CREATE TABLE hobbies (
                         id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                         name VARCHAR(50) NOT NULL,
                         category VARCHAR(30),

                         active BOOLEAN NOT NULL DEFAULT TRUE,

                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uq_hobbies_name
                             UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [05/23] user_hobbies | V1
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


-- [06/23] emotions | V1
CREATE TABLE emotions (
                          emotion_code VARCHAR(30) NOT NULL,

                          name VARCHAR(50) NOT NULL,
                          emoji VARCHAR(10),

                          base_score INT UNSIGNED NOT NULL,

                          active BOOLEAN NOT NULL DEFAULT TRUE,

                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          PRIMARY KEY (emotion_code),

                          CONSTRAINT uq_emotions_name
                              UNIQUE (name),

                          CONSTRAINT chk_emotions_base_score
                              CHECK (base_score BETWEEN 0 AND 50)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- [07/23] mood_entries | V1 + V6(current_activity)
CREATE TABLE mood_entries (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    entry_date DATE NOT NULL,
    emotion_code VARCHAR(30) NOT NULL,
    intensity INT UNSIGNED NOT NULL,
    mood_score INT UNSIGNED NOT NULL,
    current_activity VARCHAR(100) NOT NULL,
    diary_content TEXT,
    recommendation_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    recommendation_eligible_until DATETIME NULL,
    recorded_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mood_entries_user_date UNIQUE (user_id, entry_date),
    CONSTRAINT chk_mood_entries_intensity CHECK (intensity BETWEEN 1 AND 10),
    CONSTRAINT chk_mood_entries_score CHECK (mood_score BETWEEN 1 AND 60),
    CONSTRAINT chk_mood_entries_recommendation_status
        CHECK (recommendation_status IN ('AVAILABLE', 'REQUESTED', 'DECLINED', 'EXPIRED', 'DELETED')),
    CONSTRAINT fk_mood_entries_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_mood_entries_emotion
        FOREIGN KEY (emotion_code) REFERENCES emotions(emotion_code),
    INDEX idx_mood_entries_user_entry_date (user_id, entry_date),
    INDEX idx_mood_entries_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- [08/23] actions | V1 + V9(final action model)
CREATE TABLE actions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    action_name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    duration_minutes INT UNSIGNED NOT NULL,
    environment_type VARCHAR(20) NOT NULL DEFAULT 'ANY',
    social_type VARCHAR(20) NOT NULL DEFAULT 'ANY',
    activity_style VARCHAR(20) NOT NULL DEFAULT 'ANY',
    location_required BOOLEAN NOT NULL DEFAULT FALSE,
    place_category VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_actions_action_name UNIQUE (action_name),
    CONSTRAINT chk_actions_environment_type CHECK (environment_type IN ('INDOOR', 'OUTDOOR', 'ANY')),
    CONSTRAINT chk_actions_social_type CHECK (social_type IN ('ALONE', 'SOCIAL', 'ANY')),
    CONSTRAINT chk_actions_activity_style CHECK (activity_style IN ('ACTIVE', 'CALM', 'ANY')),
    CONSTRAINT chk_actions_category CHECK (
        category IN (
            'WALK', 'EXERCISE', 'STRETCHING', 'MEDITATION', 'SLEEP',
            'MUSIC', 'READING', 'ENTERTAINMENT', 'SOCIAL', 'OUTDOOR',
            'EATING', 'SELF_CARE', 'CLEANING'
        )
    ),
    CONSTRAINT chk_actions_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_actions_location_required CHECK (location_required IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [09/23] action_hobbies | V1
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


-- [10/23] user_action_dislikes | V1
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


-- [11/23] recommendation_sessions | V1 + V9, with user-deletion FK fix
CREATE TABLE recommendation_sessions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    mood_entry_id BIGINT UNSIGNED NOT NULL,
    recommendation_type VARCHAR(20) NOT NULL,
    time_bucket VARCHAR(20) NOT NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recommendation_sessions_mood_entry UNIQUE (mood_entry_id),
    CONSTRAINT fk_recommendation_sessions_mood
        FOREIGN KEY (mood_entry_id) REFERENCES mood_entries(id) ON DELETE CASCADE,
    -- 회원탈퇴 FK 보완: V9에서는 ON DELETE 동작이 지정되지 않았음
    CONSTRAINT fk_recommendation_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_recommendation_type
        CHECK (recommendation_type IN ('COLD_START', 'HYBRID', 'PERSONALIZED')),
    CONSTRAINT chk_recommendation_sessions_time_bucket
        CHECK (time_bucket IN ('MORNING', 'AFTERNOON', 'EVENING', 'NIGHT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [12/23] recommendations | V1 + V8(reason) + V9 + V11(emoji)
CREATE TABLE recommendations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT UNSIGNED NOT NULL,
    action_id BIGINT UNSIGNED NOT NULL,
    rank_no INT UNSIGNED NOT NULL,
    reason TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recheck_completed TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    selected_at DATETIME NULL DEFAULT NULL,
    selected_session_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN status = 'SELECTED' THEN session_id ELSE NULL END
        ) VIRTUAL,
    emoji VARCHAR(32) CHARACTER SET utf8mb4 NULL,
    CONSTRAINT fk_recommendations_session
        FOREIGN KEY (session_id) REFERENCES recommendation_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendations_action
        FOREIGN KEY (action_id) REFERENCES actions(id),
    CONSTRAINT uq_recommendations_session_rank UNIQUE (session_id, rank_no),
    CONSTRAINT uq_recommendations_session_action UNIQUE (session_id, action_id),
    CONSTRAINT uq_recommendations_session_id UNIQUE (session_id, id),
    CONSTRAINT uq_recommendations_id_action UNIQUE (id, action_id),
    CONSTRAINT uq_recommendations_one_selected UNIQUE (selected_session_id),
    CONSTRAINT chk_recommendations_rank CHECK (rank_no BETWEEN 1 AND 3),
    CONSTRAINT chk_recommendations_status CHECK (status IN ('PENDING', 'SELECTED', 'UNSELECTED', 'SKIPPED')),
    CONSTRAINT chk_recommendations_recheck
        CHECK (recheck_completed IN (0, 1) AND (recheck_completed = 0 OR status = 'SELECTED')),
    CONSTRAINT chk_recommendations_selected_at CHECK (
        (status = 'SELECTED' AND selected_at IS NOT NULL)
        OR (status <> 'SELECTED' AND selected_at IS NULL)
    ),
    INDEX idx_recommendations_action_id (action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [13/23] recommendation_places | V1
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


-- [14/23] action_executions | V1 + V9 + V10, with account-deletion FK fixes
CREATE TABLE action_executions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    session_id BIGINT UNSIGNED NOT NULL,
    recommendation_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'STARTED',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rechecked_at DATETIME NULL DEFAULT NULL,
    CONSTRAINT uq_action_executions_session UNIQUE (session_id),
    CONSTRAINT uq_action_executions_recommendation UNIQUE (recommendation_id),
    CONSTRAINT fk_action_executions_session
        FOREIGN KEY (session_id) REFERENCES recommendation_sessions(id) ON DELETE CASCADE,
    -- 회원탈퇴 FK 보완: V9에서는 ON DELETE 동작이 지정되지 않았음
    CONSTRAINT fk_action_executions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    -- 회원탈퇴 FK 보완: 추천 삭제 시 연결된 실행 기록도 삭제
    CONSTRAINT fk_action_executions_session_recommendation
        FOREIGN KEY (session_id, recommendation_id) REFERENCES recommendations(session_id, id) ON DELETE CASCADE,
    CONSTRAINT chk_action_executions_status
        CHECK (status IN ('STARTED', 'COMPLETED', 'CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [15/23] mood_rechecks | V1 + V10
CREATE TABLE mood_rechecks (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    action_execution_id BIGINT UNSIGNED NOT NULL,
    before_score INT UNSIGNED NOT NULL,
    after_score INT UNSIGNED NULL DEFAULT NULL,
    delta INT GENERATED ALWAYS AS (
        CAST(after_score AS SIGNED) - CAST(before_score AS SIGNED)
    ) STORED,
    checked_at DATETIME NULL DEFAULT NULL,
    CONSTRAINT uq_mood_rechecks_execution UNIQUE (action_execution_id),
    CONSTRAINT chk_mood_rechecks_before CHECK (before_score BETWEEN 1 AND 60),
    CONSTRAINT chk_mood_rechecks_after CHECK (after_score BETWEEN 1 AND 60),
    CONSTRAINT fk_mood_rechecks_execution
        FOREIGN KEY (action_execution_id) REFERENCES action_executions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [16/23] user_action_stats | V1
CREATE TABLE user_action_stats (
                                   id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                   user_id BIGINT UNSIGNED NOT NULL,

                                   action_id BIGINT UNSIGNED NOT NULL,

    -- 어떤 감정 상태에서 수행한 행동인지
                                   emotion_code VARCHAR(30) NOT NULL,

    -- 월별 통계 기준
                                   stat_year INT UNSIGNED NOT NULL,
                                   stat_month INT UNSIGNED NOT NULL,

    -- 해당 행동이 추천된 횟수
                                   recommendation_count INT UNSIGNED
        NOT NULL DEFAULT 0,

    -- 실제 행동을 실행한 횟수
                                   execution_count INT UNSIGNED
        NOT NULL DEFAULT 0,

    -- 행동 완료 + 재측정까지 완료된
    -- 실제 효과 계산 가능 표본 수
                                   sample_count INT UNSIGNED
        NOT NULL DEFAULT 0,

    -- sample 중 moodScore가 증가한 횟수
                                   positive_count INT UNSIGNED
        NOT NULL DEFAULT 0,

    -- positive_count / sample_count * 100
                                   positive_rate DECIMAL(5,2),

    -- 재측정 전후 moodScore 변화량 평균
                                   avg_delta DECIMAL(5,2),

    -- LOW / MEDIUM / HIGH
                                   confidence_level VARCHAR(20),

                                   calculated_at DATETIME
                                                            NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uq_user_action_stats
                                       UNIQUE (
                                               user_id,
                                               action_id,
                                               emotion_code,
                                               stat_year,
                                               stat_month
                                           ),

                                   CONSTRAINT fk_user_action_stats_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES users(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_user_action_stats_action
                                       FOREIGN KEY (action_id)
                                           REFERENCES actions(id),

                                   CONSTRAINT fk_user_action_stats_emotion
                                       FOREIGN KEY (emotion_code)
                                           REFERENCES emotions(emotion_code),

                                   CONSTRAINT chk_user_action_stats_month
                                       CHECK (
                                           stat_month BETWEEN 1 AND 12
                                           ),

    -- 긍정 횟수는 전체 표본 수를 넘을 수 없음
                                   CONSTRAINT chk_user_action_stats_positive_count
                                       CHECK (
                                           positive_count <= sample_count
                                           ),

    -- 재측정 완료 표본은 실행 횟수를 넘을 수 없음
                                   CONSTRAINT chk_user_action_stats_sample_count
                                       CHECK (
                                           sample_count <= execution_count
                                           ),

                                   CONSTRAINT chk_user_action_stats_positive_rate
                                       CHECK (
                                           positive_rate IS NULL
                                               OR positive_rate BETWEEN 0 AND 100
                                           ),

                                   CONSTRAINT chk_user_action_stats_confidence
                                       CHECK (
                                           confidence_level IS NULL
                                               OR confidence_level IN (
                                                                       'LOW',
                                                                       'MEDIUM',
                                                                       'HIGH'
                                               )
                                           )

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


-- [17/23] user_withdrawal_requests | V4
CREATE TABLE user_withdrawal_requests (
                                          user_id BIGINT UNSIGNED NOT NULL,

                                          requested_at DATETIME NOT NULL
                                              DEFAULT CURRENT_TIMESTAMP,

                                          deletion_scheduled_at DATETIME NOT NULL,

                                          PRIMARY KEY (user_id),

                                          CONSTRAINT fk_user_withdrawal_requests_user
                                              FOREIGN KEY (user_id)
                                                  REFERENCES users(id)
                                                  ON DELETE CASCADE
);


-- [18/23] consent_policies | V5
CREATE TABLE consent_policies (
                                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

                                  consent_type VARCHAR(50) NOT NULL,
                                  version VARCHAR(20) NOT NULL,

                                  title VARCHAR(100) NOT NULL,
                                  content TEXT NOT NULL,

                                  required BOOLEAN NOT NULL DEFAULT FALSE,
                                  active BOOLEAN NOT NULL DEFAULT TRUE,

                                  effective_at DATETIME NOT NULL,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  PRIMARY KEY (id),

                                  CONSTRAINT uk_consent_policies_type_version
                                      UNIQUE (consent_type, version)
);

CREATE INDEX idx_consent_policies_type_active
    ON consent_policies (consent_type, active);


-- [19/23] user_consents | V5
CREATE TABLE user_consents (
                               id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

                               user_id BIGINT UNSIGNED NOT NULL,
                               policy_id BIGINT UNSIGNED NOT NULL,

                               agreed BOOLEAN NOT NULL DEFAULT FALSE,

                               agreed_at DATETIME NULL,
                               revoked_at DATETIME NULL,

                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP,

                               PRIMARY KEY (id),

                               CONSTRAINT uk_user_consents_user_policy
                                   UNIQUE (user_id, policy_id),

                               CONSTRAINT fk_user_consents_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_user_consents_policy
                                   FOREIGN KEY (policy_id)
                                       REFERENCES consent_policies(id)
);


-- [20/23] social_accounts | V7
CREATE TABLE social_accounts (
                                 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

                                 user_id BIGINT UNSIGNED NOT NULL,

                                 provider VARCHAR(20) NOT NULL,

                                 provider_user_id VARCHAR(100) NOT NULL,

                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT uq_social_accounts_provider_user
                                     UNIQUE (provider, provider_user_id),

                                 CONSTRAINT uq_social_accounts_user_provider
                                     UNIQUE (user_id, provider),

                                 CONSTRAINT fk_social_accounts_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(id)
                                         ON DELETE CASCADE,

                                 INDEX idx_social_accounts_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [21/23] action_category_scores | V9, with user-deletion FK fix
CREATE TABLE action_category_scores (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    category VARCHAR(30) NOT NULL,
    personal_score DECIMAL(5,2) NOT NULL DEFAULT 50.00,
    avg_delta DECIMAL(6,2) NULL DEFAULT NULL,
    positive_count INT UNSIGNED NOT NULL DEFAULT 0,
    sample_count INT UNSIGNED NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_action_category_scores_user_category UNIQUE (user_id, category),
    -- 회원탈퇴 FK 보완: V9에서는 ON DELETE 동작이 지정되지 않았음
    CONSTRAINT fk_action_category_scores_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_action_category_scores_category CHECK (
        category IN (
            'WALK', 'EXERCISE', 'STRETCHING', 'MEDITATION', 'SLEEP',
            'MUSIC', 'READING', 'ENTERTAINMENT', 'SOCIAL', 'OUTDOOR',
            'EATING', 'SELF_CARE', 'CLEANING'
        )
    ),
    CONSTRAINT chk_action_category_scores_score CHECK (personal_score BETWEEN 0 AND 100),
    CONSTRAINT chk_action_category_scores_delta CHECK (avg_delta BETWEEN -59 AND 59),
    CONSTRAINT chk_action_category_scores_counts CHECK (positive_count <= sample_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- [22/23] action_categories | V11
CREATE TABLE action_categories (
                                   category_code VARCHAR(30) NOT NULL,
                                   category_name VARCHAR(50) NOT NULL,
                                   emoji VARCHAR(32) NOT NULL,

                                   PRIMARY KEY (category_code)
) DEFAULT CHARACTER SET utf8mb4;


-- [23/23] email_verifications | V12
CREATE TABLE email_verifications (
                                     id BIGINT NOT NULL AUTO_INCREMENT,

                                     email VARCHAR(255) NOT NULL,
                                     code_hash VARCHAR(255) NOT NULL,

                                     expires_at DATETIME(6) NOT NULL,
                                     attempt_count INT NOT NULL DEFAULT 0,

                                     verified_at DATETIME(6) NULL,

                                     signup_token_hash CHAR(64) NULL,
                                     signup_token_expires_at DATETIME(6) NULL,

                                     used_at DATETIME(6) NULL,
                                     last_sent_at DATETIME(6) NOT NULL,

                                     created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                     updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_email_verifications_email (email),
                                     INDEX idx_email_verifications_expires (expires_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
