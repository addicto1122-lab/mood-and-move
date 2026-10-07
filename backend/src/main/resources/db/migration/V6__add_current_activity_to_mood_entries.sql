ALTER TABLE mood_entries
    ADD COLUMN current_activity VARCHAR(100) NOT NULL
    AFTER mood_score;