import { useEffect, useRef, useState } from "react";

import { useLocation, useNavigate } from "react-router-dom";

import { generateRecommendations } from "../api/recommendationApi";

import "./RecommendationPage.css";

const environmentNames = {
  INDOOR: "실내",
  OUTDOOR: "실외",
  ANY: "어디서든"
};

export default function RecommendationPage() {
  const navigate = useNavigate();
  const location = useLocation();

  /*
   * MoodWritePage에서 전달된 값
   */
  const { moodEntryId, locationMode, currentLocation } = location.state ?? {};

  const [recommendations, setRecommendations] = useState([]);

  const [loading, setLoading] = useState(true);

  const [error, setError] = useState("");

  /*
   * React StrictMode에서
   * 추천 API 중복 호출 방지
   */
  const requestedRef = useRef(false);

  /*
   * 추천 생성
   */
  useEffect(() => {
    if (!moodEntryId) {
      return;
    }

    if (requestedRef.current) {
      return;
    }

    requestedRef.current = true;

    async function loadRecommendations() {
      try {
        setLoading(true);
        setError("");

        const data = await generateRecommendations({
          moodEntryId,
          locationMode,
          currentLocation
        });

        setRecommendations(data.recommendations ?? []);
      } catch (error) {
        console.error(error);

        setError(error.message || "추천 행동을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }

    loadRecommendations();
  }, [moodEntryId, locationMode, currentLocation]);

  /*
   * 추천 행동 시작
   *
   * 추후 ActionExecution API 연결
   */
  const handleStart = (item) => {
    console.log("선택한 추천", {
      recommendationId: item.recommendationId,
      actionId: item.actionId,
      actionName: item.actionName
    });

    alert(`${item.actionName}을(를) 시작합니다.`);

    /*
     * TODO
     *
     * ActionExecution API 연결
     */
  };

  /*
   * 추천 건너뛰기
   *
   * 추후 RecommendationSession
   * selectionStatus 변경 API 연결
   */
  const handleSkip = () => {
    const skip = window.confirm("이번 추천을 건너뛸까요?");

    if (!skip) {
      return;
    }

    navigate("/");
  };

  /*
   * 추천할 감정 기록 없이
   * URL로 직접 진입한 경우
   */
  if (!moodEntryId) {
    return (
      <main className="recommendation-page">
        <header className="recommendation-header">
          <div>
            <span className="recommendation-eyebrow">맞춤 행동 추천</span>

            <h1>추천할 감정 기록이 없어요.</h1>

            <p>오늘의 감정을 먼저 기록해주세요.</p>

            <button type="button" onClick={() => navigate("/mood")}>
              감정 기록하기
            </button>
          </div>
        </header>
      </main>
    );
  }

  /*
   * 추천 생성 중
   */
  if (loading) {
    return (
      <main className="recommendation-page">
        <header className="recommendation-header">
          <div>
            <span className="recommendation-eyebrow">맞춤 행동 추천</span>

            <h1>행동을 고르고 있어요...</h1>

            <p>
              지금의 감정과 상황을 살펴보고
              <br />
              부담 없이 할 수 있는 행동을 찾고 있어요.
            </p>
          </div>
        </header>
      </main>
    );
  }

  /*
   * 추천 API 실패
   */
  if (error) {
    return (
      <main className="recommendation-page">
        <header className="recommendation-header">
          <div>
            <span className="recommendation-eyebrow">추천 오류</span>

            <h1>추천을 불러오지 못했어요.</h1>

            <p>{error}</p>

            <button type="button" onClick={() => navigate("/mood")}>
              다시 작성하기
            </button>
          </div>
        </header>
      </main>
    );
  }

  return (
    <main className="recommendation-page">
      <header className="recommendation-header">
        <button
          type="button"
          className="recommendation-back-button"
          onClick={() => navigate(-1)}
          aria-label="뒤로가기"
        >
          ‹
        </button>

        <div>
          <span className="recommendation-eyebrow">맞춤 행동 추천</span>

          <h1>이런 행동은 어때요?</h1>

          <p>
            지금의 감정과 상황을 고려해
            <br />
            부담 없이 할 수 있는 행동을 골랐어요.
          </p>
        </div>
      </header>

      <section className="recommendation-grid">
        {recommendations.map((item) => (
          <article
            key={item.recommendationId}
            className={
              item.rankNo === 1
                ? "action-recommend-card best"
                : "action-recommend-card"
            }
          >
            <div className="card-top">
              <div className="card-icon">✨</div>

              {item.rankNo === 1 && (
                <span className="best-badge">✨ 가장 잘 맞아요</span>
              )}
            </div>

            <h2>{item.actionName}</h2>

            <div className="card-meta">
              <span>◷ {item.durationMinutes}분</span>

              <span>
                ⌖ {environmentNames[item.environmentType] ?? "어디서든"}
              </span>
            </div>

            <p className="card-reason">{item.reason}</p>

            <button
              type="button"
              className="card-start-button"
              onClick={() => handleStart(item)}
            >
              이 행동 하기
            </button>
          </article>
        ))}
      </section>

      <button
        type="button"
        className="recommendation-skip-button"
        onClick={handleSkip}
      >
        이번 추천 건너뛰기
      </button>
    </main>
  );
}
