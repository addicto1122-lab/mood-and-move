import { useEffect, useRef, useState } from "react";

import { useLocation, useNavigate } from "react-router-dom";

import { generateRecommendations } from "../api/recommendationApi";

import "./RecommendationPage.css";

/*
 * 현재 Backend LLM 응답은
 *
 * actionCode
 * reason
 *
 * 만 반환하기 때문에
 * 화면 표시용 Action 정보는 임시 매핑.
 *
 * 나중에는 Backend Response에서
 * Action 정보까지 내려주도록 변경 예정.
 */
const ACTION_META = {
  WALK_PARK: {
    emoji: "🌿",
    name: "공원 산책",
    durationMinutes: 15,
    environmentType: "OUTDOOR",
    locationRequired: true,
  },

  STRETCH: {
    emoji: "🧘",
    name: "가벼운 스트레칭",
    durationMinutes: 5,
    environmentType: "INDOOR",
    locationRequired: false,
  },

  LISTEN_MUSIC: {
    emoji: "🎧",
    name: "음악 듣기",
    durationMinutes: 10,
    environmentType: "ANY",
    locationRequired: false,
  },

  DEEP_BREATH: {
    emoji: "🌬️",
    name: "심호흡하기",
    durationMinutes: 5,
    environmentType: "ANY",
    locationRequired: false,
  },
};

const environmentNames = {
  INDOOR: "실내",
  OUTDOOR: "실외",
  ANY: "어디서든",
};

export default function RecommendationPage() {
  const navigate = useNavigate();
  const location = useLocation();

  /*
   * MoodWritePage에서 navigate로 넘긴 값
   */
  const { moodEntryId, locationMode, currentLocation } = location.state ?? {};

  const [recommendations, setRecommendations] = useState([]);

  const [loading, setLoading] = useState(true);

  const [error, setError] = useState("");

  /*
   * React StrictMode 개발 환경에서
   * API가 2번 호출되는 것을 방지
   */
  const requestedRef = useRef(false);

  /*
   * 추천 페이지 진입 즉시
   * Gemini 추천 요청
   */
  useEffect(() => {
    if (requestedRef.current) {
      return;
    }

    requestedRef.current = true;

    /*
     * /recommendation 주소를
     * 직접 입력해서 들어온 경우
     */
    if (!moodEntryId) {
      setError("추천할 감정 기록이 없습니다.");

      setLoading(false);

      return;
    }

    async function loadRecommendations() {
      try {
        setLoading(true);
        setError("");

        const data = await generateRecommendations({
          moodEntryId,

          locationMode,

          currentLocation,
        });

        /*
         * 현재 Backend 응답
         *
         * {
         *   recommendations: [
         *      {
         *        actionCode: "...",
         *        reason: "..."
         *      }
         *   ]
         * }
         */

        const converted = (data.recommendations ?? []).map((item, index) => {
          const meta = ACTION_META[item.actionCode];

          /*
           * 혹시 모르는 actionCode가 오더라도
           * 화면 자체는 죽지 않도록 처리
           */
          return {
            recommendationId: `${item.actionCode}-${index}`,

            rankNo: index + 1,

            actionCode: item.actionCode,

            reason: item.reason,

            emoji: meta?.emoji ?? "✨",

            name: meta?.name ?? item.actionCode,

            durationMinutes: meta?.durationMinutes ?? 5,

            environmentType: meta?.environmentType ?? "ANY",

            locationRequired: meta?.locationRequired ?? false,

            /*
             * 아직 Kakao 장소 연결 전
             */
            place: null,
          };
        });

        setRecommendations(converted);
      } catch (error) {
        console.error(error);

        setError(error.message || "추천 행동을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }

    loadRecommendations();
  }, [moodEntryId, locationMode, currentLocation]);

  const handleStart = (item) => {
    console.log("선택한 추천", {
      actionCode: item.actionCode,
    });

    alert(`${item.name}을(를) 시작합니다.`);

    /*
     * TODO
     *
     * ActionExecution API 연결
     */
  };

  const handleSkip = () => {
    const skip = window.confirm("이번 추천을 건너뛸까요?");

    if (!skip) {
      return;
    }

    navigate("/");
  };

  /*
   * Gemini 응답 기다리는 동안
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
   * API 실패
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
              <div className="card-icon">{item.emoji}</div>

              {item.rankNo === 1 && (
                <span className="best-badge">✨ 가장 잘 맞아요</span>
              )}
            </div>

            <h2>{item.name}</h2>

            <div className="card-meta">
              <span>◷ {item.durationMinutes}분</span>

              <span>⌖ {environmentNames[item.environmentType]}</span>
            </div>

            <p className="card-reason">{item.reason}</p>

            {item.locationRequired && item.place && (
              <div className="place-box">
                <div className="place-icon">⌖</div>

                <div>
                  <strong>가까운 장소</strong>

                  <span>
                    {item.place.name}
                    {" · "}
                    {item.place.distance}
                  </span>
                </div>
              </div>
            )}

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
