-- =========================================================
-- Mood&Move
-- V2__seed_master_data.sql
-- =========================================================


-- =========================================================
-- 1. 감정 종류 Master
-- 담당: 이래원
-- =========================================================
INSERT INTO emotions (
    emotion_code,
    name,
    emoji,
    base_score
)
VALUES
    ('ANGRY',   '화남',   '😡', 0),
    ('ANXIOUS', '불안',   '😰', 10),
    ('SAD',     '슬픔',   '😢', 20),
    ('NEUTRAL', '보통',   '😐', 30),
    ('CALM',    '편안함', '🙂', 40),
    ('JOY',     '기쁨',   '😄', 50);



-- =========================================================
-- 2. 취미 Master
-- 담당: 이래원
-- =========================================================
INSERT INTO hobbies (
    name,
    category
)
VALUES
    ('산책', 'ACTIVITY'),
    ('독서', 'REST'),
    ('음악', 'REST'),
    ('게임', 'ENTERTAINMENT'),
    ('운동', 'ACTIVITY');



-- =========================================================
-- 3. 현재 활동 Master
-- 담당: 이래원
-- =========================================================
INSERT INTO activity_tags (
    name,
    category
)
VALUES
    ('공부', 'WORK'),
    ('프로젝트', 'WORK'),
    ('업무', 'WORK'),
    ('운동', 'ACTIVITY'),
    ('휴식', 'REST'),
    ('이동', 'MOVE'),
    ('친구 만남', 'SOCIAL');



-- =========================================================
-- 4. 추천 행동 Master
-- 담당: 장준호
-- =========================================================
INSERT INTO actions (
    action_code,
    name,
    category,
    duration_minutes,
    environment_type,
    social_type,
    activity_style,
    location_required,
    place_category
)
VALUES
    (
        'WALK_PARK',
        '공원 산책',
        'WALK',
        15,
        'OUTDOOR',
        'ANY',
        'ACTIVE',
        TRUE,
        'PARK'
    ),
    (
        'STRETCH',
        '가벼운 스트레칭',
        'EXERCISE',
        5,
        'INDOOR',
        'ALONE',
        'ACTIVE',
        FALSE,
        NULL
    ),
    (
        'LISTEN_MUSIC',
        '음악 듣기',
        'REST',
        10,
        'ANY',
        'ALONE',
        'CALM',
        FALSE,
        NULL
    ),
    (
        'DEEP_BREATH',
        '심호흡하기',
        'REST',
        5,
        'ANY',
        'ALONE',
        'CALM',
        FALSE,
        NULL
    );



-- =========================================================
-- 5. ACTION <-> HOBBY 매핑
-- 담당: 장준호
-- =========================================================

INSERT INTO action_hobbies (
    action_id,
    hobby_id
)
SELECT
    a.id,
    h.id
FROM actions a
         JOIN hobbies h
              ON h.name = '산책'
WHERE a.action_code = 'WALK_PARK';


INSERT INTO action_hobbies (
    action_id,
    hobby_id
)
SELECT
    a.id,
    h.id
FROM actions a
         JOIN hobbies h
              ON h.name = '음악'
WHERE a.action_code = 'LISTEN_MUSIC';


INSERT INTO action_hobbies (
    action_id,
    hobby_id
)
SELECT
    a.id,
    h.id
FROM actions a
         JOIN hobbies h
              ON h.name = '운동'
WHERE a.action_code = 'STRETCH';