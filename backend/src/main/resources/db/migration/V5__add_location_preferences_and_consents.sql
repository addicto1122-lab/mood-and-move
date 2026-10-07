-- =========================================================
-- 1. 사용자 기본 활동 지역
-- =========================================================

ALTER TABLE user_preferences
    ADD COLUMN default_region_name VARCHAR(100) NULL,
    ADD COLUMN default_region_code VARCHAR(30) NULL,
    ADD COLUMN default_region_latitude DECIMAL(10, 7) NULL,
    ADD COLUMN default_region_longitude DECIMAL(10, 7) NULL;


-- =========================================================
-- 2. 약관 마스터
-- 어떤 약관 / 어떤 버전인지 관리
-- =========================================================

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


-- =========================================================
-- 3. 사용자 약관 동의 기록
-- =========================================================

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


CREATE INDEX idx_consent_policies_type_active
    ON consent_policies (consent_type, active);


-- =========================================================
-- 4. 현재 위치 기반 추천 선택 약관
-- =========================================================

INSERT INTO consent_policies (
    consent_type,
    version,
    title,
    content,
    required,
    active,
    effective_at
)
VALUES (
           'CURRENT_LOCATION',
           '1.0',
           '현재 위치 기반 추천 동의',
           '현재 위치를 이용하여 주변의 활동 장소를 추천합니다. 현재 위치는 장소 추천을 위한 요청 시 사용하며 기본 활동 지역으로 저장하지 않습니다.',
           FALSE,
           TRUE,
           NOW()
       );