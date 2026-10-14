-- 추천마다 장소 검색 정보를 따로 저장
ALTER TABLE recommendations
    ADD COLUMN location_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN place_category VARCHAR(50) NULL,
    ADD COLUMN place_keyword VARCHAR(50) NULL;

-- 기존 추천의 장소 설정 유지
UPDATE recommendations r
    JOIN actions a ON a.id = r.action_id
    JOIN recommendation_sessions s ON s.id = r.session_id
    SET
        r.location_required = CASE
        WHEN a.category <> 'TRAVEL'
        AND s.location_mode <> 'NONE'
        AND a.location_required = TRUE
        AND a.place_category IS NOT NULL
        THEN TRUE
        ELSE FALSE
END,
    r.place_category = CASE
        WHEN a.category <> 'TRAVEL'
             AND s.location_mode <> 'NONE'
             AND a.location_required = TRUE
             AND a.place_category IS NOT NULL
        THEN a.place_category
        ELSE NULL
END;