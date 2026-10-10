-- 사용하지 않는 실행 시각 컬럼 삭제
ALTER TABLE action_executions
    DROP COLUMN created_at,
    DROP COLUMN updated_at,
    DROP COLUMN completed_at;

-- 선택하는 순간 started_at이 생성되므로
-- NOT NULL 유지
ALTER TABLE action_executions
    MODIFY COLUMN started_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP;

-- 재측정 전에는 NULL
ALTER TABLE action_executions
    MODIFY COLUMN rechecked_at DATETIME NULL DEFAULT NULL;

-- 선택 시에는 재측정 전 점수만 저장
-- 선택 직후: before_score만 저장하고 나머지는 NULL.
-- 재측정 완료: after_score, checked_at을 채우면 DB가 delta를 자동 계산.
-- 점수가 내려갔을 때: SIGNED로 변환해서 음수 변화량도 계산할 수 있게 처리.
ALTER TABLE mood_rechecks
    MODIFY COLUMN after_score INT UNSIGNED NULL DEFAULT NULL,
    MODIFY COLUMN checked_at DATETIME NULL DEFAULT NULL,
    MODIFY COLUMN delta INT GENERATED ALWAYS AS (
    CAST(after_score AS SIGNED) - CAST(before_score AS SIGNED)
    ) STORED;