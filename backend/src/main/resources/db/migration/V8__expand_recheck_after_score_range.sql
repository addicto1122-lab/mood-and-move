ALTER TABLE mood_rechecks
DROP CHECK chk_mood_rechecks_after;

ALTER TABLE mood_rechecks
    ADD CONSTRAINT chk_mood_rechecks_after
        CHECK (after_score BETWEEN 0 AND 100);

ALTER TABLE action_category_scores
DROP CHECK chk_action_category_scores_delta;

ALTER TABLE action_category_scores
    ADD CONSTRAINT chk_action_category_scores_delta
        CHECK (avg_delta BETWEEN -60 AND 99);

-- ============================================================
-- 행동 카테고리 17개로 개편
-- SLEEP → REST
-- DRINK, SOCIAL, REST, COOKING 추가
-- ============================================================

-- 1. 기존 카테고리 제한 해제
ALTER TABLE actions
DROP CHECK chk_actions_category;

ALTER TABLE action_category_scores
DROP CHECK chk_action_category_scores_category;


-- 2. 신규 카테고리 등록
INSERT INTO action_categories (
    category_code,
    category_name,
    emoji
)
VALUES
    ('DRINK',   '차/음료', '☕'),
    ('SOCIAL',  '친목',    '💬'),
    ('REST',    '휴식',    '😴'),
    ('COOKING', '요리',    '🍳');


-- 3. 기존 수면 행동과 개인화 통계를 휴식으로 이전
-- 기존 제약조건상 REST는 없으므로 SLEEP 통계를 그대로 이전
UPDATE actions
SET category = 'REST'
WHERE category = 'SLEEP';

UPDATE action_category_scores
SET category = 'REST'
WHERE category = 'SLEEP';

DELETE FROM action_categories
WHERE category_code = 'SLEEP';


-- 4. 기존 카테고리 표시 이름 변경
UPDATE action_categories
SET category_name = CASE category_code
                        WHEN 'ENTERTAINMENT' THEN '게임/오락'
                        WHEN 'SELF_CARE'     THEN '자기 관리'
                        WHEN 'MEDITATION'    THEN '명상/호흡'
                        WHEN 'EATING'        THEN '식사/간식'
                        WHEN 'TRAVEL'        THEN '여행/나들이'
                        WHEN 'CLEANING'      THEN '정리/청소'
                        WHEN 'CULTURE'       THEN '문화/체험'
                        ELSE category_name
    END
WHERE category_code IN (
                        'ENTERTAINMENT',
                        'SELF_CARE',
                        'MEDITATION',
                        'EATING',
                        'TRAVEL',
                        'CLEANING',
                        'CULTURE'
    );


-- 5. 행동 테이블의 허용 카테고리 변경
ALTER TABLE actions
    ADD CONSTRAINT chk_actions_category CHECK (
        category IN (
                     'ENTERTAINMENT', 'WALK', 'EXERCISE',
                     'STRETCHING', 'OUTDOOR', 'SELF_CARE',
                     'READING', 'MUSIC', 'MEDITATION',
                     'EATING', 'DRINK', 'TRAVEL',
                     'CLEANING', 'CULTURE', 'SOCIAL',
                     'REST', 'COOKING'
            )
        );


-- 6. 개인화 통계 테이블의 허용 카테고리 변경
ALTER TABLE action_category_scores
    ADD CONSTRAINT chk_action_category_scores_category CHECK (
        category IN (
                     'ENTERTAINMENT', 'WALK', 'EXERCISE',
                     'STRETCHING', 'OUTDOOR', 'SELF_CARE',
                     'READING', 'MUSIC', 'MEDITATION',
                     'EATING', 'DRINK', 'TRAVEL',
                     'CLEANING', 'CULTURE', 'SOCIAL',
                     'REST', 'COOKING'
            )
        );