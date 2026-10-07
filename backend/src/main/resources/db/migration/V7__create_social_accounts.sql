ALTER TABLE users
    ADD COLUMN local_login_enabled BOOLEAN NOT NULL DEFAULT TRUE
    AFTER password_hash;

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