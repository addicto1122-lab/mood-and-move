-- 추천 요청 당시의 장소 검색 기준 좌표
ALTER TABLE recommendation_sessions
    ADD COLUMN search_latitude DECIMAL(10, 7) NULL,
    ADD COLUMN search_longitude DECIMAL(10, 7) NULL;