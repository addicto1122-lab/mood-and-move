ALTER TABLE mood_rechecks
DROP CHECK chk_mood_rechecks_after;

ALTER TABLE mood_rechecks
    ADD CONSTRAINT chk_mood_rechecks_after
        CHECK (after_score BETWEEN 0 AND 80);

ALTER TABLE action_category_scores
DROP CHECK chk_action_category_scores_delta;

ALTER TABLE action_category_scores
    ADD CONSTRAINT chk_action_category_scores_delta
        CHECK (avg_delta BETWEEN -60 AND 79);
