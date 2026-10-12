ALTER TABLE recommendation_sessions
    ADD COLUMN location_mode VARCHAR(20) NOT NULL DEFAULT 'NONE';