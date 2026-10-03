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

CREATE INDEX idx_user_withdrawal_requests_deletion_scheduled_at
    ON user_withdrawal_requests (deletion_scheduled_at);