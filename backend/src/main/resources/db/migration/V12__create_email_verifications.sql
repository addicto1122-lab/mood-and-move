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