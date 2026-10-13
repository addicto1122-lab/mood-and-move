-- ============================================================

-- Mood&Move | V2__seed_master_data.sql (consolidated from V1-V12)

-- For the consolidated V1 schema only. Contains data inserts, no DDL.

-- Old V2 actions/action_hobbies were deleted in V9, so those

-- obsolete rows are intentionally NOT reintroduced.

-- ============================================================


-- 1. Emotion master data (original V2)

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


-- 2. Hobby master data (original V2)

INSERT INTO hobbies (
    name,
    category
)
VALUES
    ('산책', 'WALK'),
    ('독서', 'READING'),
    ('음악', 'MUSIC'),
    ('오락', 'ENTERTAINMENT'),
    ('운동', 'ACTIVITY');


-- 3. Current-location consent policy (original V5)

INSERT INTO consent_policies (
    consent_type,
    version,
    title,
    content,
    required,
    active,
    effective_at
)
VALUES (
           'CURRENT_LOCATION',
           '1.0',
           '현재 위치 기반 추천 동의',
           '현재 위치를 이용하여 주변의 활동 장소를 추천합니다. 현재 위치는 장소 추천을 위한 요청 시 사용하며 기본 활동 지역으로 저장하지 않습니다.',
           FALSE,
           TRUE,
           NOW()
       );


-- 4. Action category master data (original V11)

INSERT INTO action_categories (
    category_code,
    category_name,
    emoji
) VALUES
      ('WALK',          '산책',        '🚶'),
      ('EXERCISE',      '운동',        '💪'),
      ('STRETCHING',    '스트레칭',    '🤸'),
      ('MEDITATION',    '명상',        '🧘'),
      ('SLEEP',         '수면 관리',   '😴'),
      ('MUSIC',         '음악',        '🎵'),
      ('READING',       '독서',        '📖'),
      ('ENTERTAINMENT', '오락',        '🎮'),
      ('SOCIAL',        '사회적 교류', '💬'),
      ('OUTDOOR',       '야외 활동',   '🌳'),
      ('EATING',        '음식 섭취',   '🍽️'),
      ('SELF_CARE',     '자기 관리',   '🛁'),
      ('CLEANING',      '정리·청소',   '🧹');
