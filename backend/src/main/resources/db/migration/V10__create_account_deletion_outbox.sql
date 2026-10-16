CREATE TABLE account_deletion_outbox (
                                         id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                         user_id BIGINT UNSIGNED NOT NULL,

                                         attempt_count INT NOT NULL DEFAULT 0,
                                         next_attempt_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                         last_error VARCHAR(1000) NULL,
                                         created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                         PRIMARY KEY (id),

                                         CONSTRAINT uk_account_deletion_outbox_user
                                             UNIQUE (user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_account_deletion_outbox_due
    ON account_deletion_outbox (
                                next_attempt_at,
                                id
        );