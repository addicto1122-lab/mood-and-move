
-- ============================================================
-- V4: 비밀번호 재설정 이메일 인증 테이블
-- ============================================================

CREATE TABLE password_reset_verifications (
                                              id BIGINT NOT NULL AUTO_INCREMENT,

    -- 비밀번호 재설정 대상 이메일
                                              email VARCHAR(255) NOT NULL,

    -- 6자리 이메일 인증번호 (BCrypt 해시)
                                              code_hash VARCHAR(255) NOT NULL,

    -- 인증번호 만료 시각 (발급 후 5분)
                                              expires_at DATETIME(6) NOT NULL,

    -- 인증번호 검증 실패 횟수
                                              attempt_count INT NOT NULL DEFAULT 0,

    -- 이메일 인증 완료 시각
                                              verified_at DATETIME(6) NULL,

    -- 비밀번호 재설정용 일회성 토큰 (SHA-256 해시)
                                              reset_token_hash CHAR(64) NULL,

    -- 재설정 토큰 만료 시각 (발급 후 10분)
                                              reset_token_expires_at DATETIME(6) NULL,

    -- 재설정 완료 시각 (토큰 재사용 방지)
                                              used_at DATETIME(6) NULL,

    -- 마지막 인증번호 발송 시각
                                              last_sent_at DATETIME(6) NOT NULL,

    -- 생성 및 수정 시각
                                              created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                              updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                              PRIMARY KEY (id),

                                              CONSTRAINT uk_password_reset_email
                                                  UNIQUE (email),

                                              UNIQUE KEY uk_password_reset_token
                                                  (reset_token_hash),

                                              INDEX idx_password_reset_expires
                                                  (expires_at),

                                              INDEX idx_password_reset_token_expires
                                                  (reset_token_expires_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
