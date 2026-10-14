ALTER TABLE recommendation_sessions
    ADD COLUMN selection_status VARCHAR(20) NOT NULL DEFAULT 'GENERATED';