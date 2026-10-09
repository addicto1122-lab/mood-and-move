import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import "./MoodRecheckPage.css";

import {
  getActionExecution,
  recheckActionExecution,
} from "../api/recommendationApi";

export default function MoodRecheckPage() {
  const navigate = useNavigate();
  const { executionId } = useParams();

  const [execution, setExecution] = useState(null);
  const [afterScore, setAfterScore] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submittingRef = useRef(false);

  // 실행 기록 조회 및 슬라이더 초기값 설정
  useEffect(() => {
    let cancelled = false;

    async function loadExecution() {
      try {
        setLoading(true);
        setLoadError("");
        setSubmitError("");

        const data = await getActionExecution(executionId);

        if (cancelled) {
          return;
        }

        setExecution(data);

        // 완료된 기록이면 저장된 결과, 아니면 행동 전 점수
        setAfterScore(data.afterScore ?? data.beforeScore);
      } catch (err) {
        if (!cancelled) {
          setLoadError(err.message || "행동 기록을 불러오지 못했습니다.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadExecution();

    return () => {
      cancelled = true;
    };
  }, [executionId]);

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (submittingRef.current || !execution || execution.status !== "STARTED") {
      return;
    }

    if (!Number.isInteger(afterScore) || afterScore < 1 || afterScore > 60) {
      setSubmitError("기분 점수는 1~60 사이로 선택해 주세요.");
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setSubmitError("");

    try {
      const result = await recheckActionExecution(executionId, afterScore);

      // 실제 저장된 결과로 화면 갱신
      setExecution(result);
      setAfterScore(result.afterScore);
    } catch (err) {
      setSubmitError(err.message || "기분 변화를 저장하지 못했습니다.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <main className="recheck-page">
        <p role="status">행동 기록을 불러오고 있어요.</p>
      </main>
    );
  }

  if (loadError || !execution) {
    return (
      <main className="recheck-page">
        <h1>기록을 불러오지 못했어요.</h1>
        <p role="alert">{loadError || "행동 기록이 없습니다."}</p>

        <button type="button" onClick={() => window.location.reload()}>
          다시 시도
        </button>

        <button type="button" onClick={() => navigate("/")}>
          메인으로
        </button>
      </main>
    );
  }

  const completed = execution.status === "COMPLETED";
  const canRecheck = execution.status === "STARTED";

  const emoji =
    execution.emoji?.trim() || execution.categoryEmoji?.trim() || "✨";

  return (
    <main className="recheck-page">
      <header className="recheck-header">
        <h1>{completed ? "기분 변화를 기록했어요" : "행동은 어떠셨나요?"}</h1>
      </header>

      <section className="recheck-action">
        <span className="recheck-emoji" aria-hidden="true">
          {emoji}
        </span>

        <h2>{execution.actionName}</h2>

        <p>
          {execution.categoryName ? `${execution.categoryName} · ` : ""}
          {execution.durationMinutes}분
        </p>
      </section>

      {!completed && (
        <section className="recheck-before">
          <h2>행동 전 기분</h2>
          <p>{execution.beforeScore}점</p>
        </section>
      )}

      {completed ? (
        <section className="recheck-result" aria-live="polite">
          <span className="recheck-result-label">나의 기분 변화</span>

          <div className="recheck-comparison">
            <div className="recheck-score-column">
              <span>행동 전</span>
              <div className="recheck-score-number">
                {execution.beforeScore}
                <small>점</small>
              </div>
            </div>

            <span className="recheck-score-arrow" aria-hidden="true">
              →
            </span>

            <div className="recheck-score-column is-after">
              <span>행동 후</span>
              <div className="recheck-score-number">
                {execution.afterScore}
                <small>점</small>
              </div>
            </div>
          </div>

          <div className="recheck-change">
            <span className="recheck-change-badge">
              {execution.delta > 0 ? "+" : ""}
              {execution.delta}점
            </span>

            <p>
              {execution.delta > 0
                ? "행동 전보다 기분 점수가 올라갔어요."
                : execution.delta < 0
                  ? "행동 전보다 기분 점수가 내려갔어요."
                  : "행동 전과 같은 기분 점수를 기록했어요."}
            </p>
          </div>

          <p className="recheck-result-message">
            {execution.delta > 0
              ? "오늘의 작은 실천과 변화를 기록해 두었어요."
              : "어떤 변화든 괜찮아요. 지금의 마음을 기록해 두었어요."}
          </p>

          <button
            type="button"
            className="recheck-submit"
            onClick={() => navigate("/", { replace: true })}
          >
            메인으로 돌아가기
          </button>
        </section>
      ) : canRecheck ? (
        <form onSubmit={handleSubmit}>
          <label htmlFor="after-score">지금 기분은 어떤가요?</label>

          <div className="recheck-slider">
            <span>1</span>

            <input
              id="after-score"
              type="range"
              min="1"
              max="60"
              step="1"
              value={afterScore ?? execution.beforeScore}
              onChange={(event) => setAfterScore(Number(event.target.value))}
              disabled={submitting}
              aria-valuetext={`${afterScore}점`}
            />

            <span>60</span>
          </div>

          <output htmlFor="after-score" className="recheck-score">
            {afterScore}점
          </output>

          <p className="recheck-hint">
            1점은 기분이 매우 좋지 않은 상태, 60점은 매우 좋은 상태예요.
          </p>

          {submitError && <p role="alert">{submitError}</p>}

          <button
            type="submit"
            className="recheck-submit"
            disabled={submitting}
          >
            {submitting ? "저장 중..." : "기분 변화 기록하기"}
          </button>
        </form>
      ) : (
        <p>현재 상태에서는 재측정할 수 없는 행동이에요.</p>
      )}

      {!completed && (
        <button
          type="button"
          className="recheck-back"
          disabled={submitting}
          onClick={() => navigate("/")}
        >
          메인으로 돌아가기
        </button>
      )}
    </main>
  );
}
