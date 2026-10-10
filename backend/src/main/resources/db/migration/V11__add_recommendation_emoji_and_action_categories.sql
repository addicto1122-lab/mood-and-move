-- 1. LLM이 생성한 추천 행동별 이모지
ALTER TABLE recommendations
    ADD COLUMN emoji VARCHAR(32)
        CHARACTER SET utf8mb4
    NULL;

-- 2. 카테고리 코드·한국어 라벨·대표 이모지
CREATE TABLE action_categories (
                                   category_code VARCHAR(30) NOT NULL,
                                   category_name VARCHAR(50) NOT NULL,
                                   emoji VARCHAR(32) NOT NULL,

                                   PRIMARY KEY (category_code)
) DEFAULT CHARACTER SET utf8mb4;

-- 3. 카테고리 기본 데이터
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