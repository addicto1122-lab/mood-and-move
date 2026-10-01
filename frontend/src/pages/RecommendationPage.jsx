import { useNavigate } from "react-router-dom";
import "./RecommendationPage.css";

const recommendations = [
  {
    recommendationId: 1,
    actionId: 1,
    rankNo: 1,
    emoji: "🌿",
    name: "공원 산책",
    durationMinutes: 15,
    environmentType: "OUTDOOR",
    reason: "복잡한 생각에서 잠시 벗어나 기분을 환기하는 데 도움이 돼요.",
    locationRequired: true,
    place: {
      name: "한빛공원",
      distance: "도보 4분"
    }
  },
  {
    recommendationId: 2,
    actionId: 2,
    rankNo: 2,
    emoji: "🧘",
    name: "목과 어깨 스트레칭",
    durationMinutes: 5,
    environmentType: "INDOOR",
    reason: "오래 앉아 있어 굳은 몸을 가볍게 풀어보세요.",
    locationRequired: false,
    place: null
  },
  {
    recommendationId: 3,
    actionId: 3,
    rankNo: 3,
    emoji: "🎧",
    name: "좋아하는 음악 한 곡",
    durationMinutes: 4,
    environmentType: "ANY",
    reason: "익숙한 음악으로 마음의 리듬을 편안하게 바꿔봐요.",
    locationRequired: false,
    place: null
  }
];

const environmentNames = {
  INDOOR: "실내",
  OUTDOOR: "실외",
  ANY: "어디서든"
};

export default function RecommendationPage() {
  const navigate = useNavigate();

  const handleStart = (item) => {
    console.log("선택한 추천", {
      recommendationId: item.recommendationId,
      actionId: item.actionId
    });

    // TODO:
    // 백엔드 행동 시작 API 연결
    //
    // await fetch("/api/action-executions", {
    //   method: "POST",
    //   headers: {
    //     "Content-Type": "application/json",
    //   },
    //   credentials: "include",
    //   body: JSON.stringify({
    //     recommendationId: item.recommendationId,
    //   }),
    // });

    alert(`${item.name}을(를) 시작합니다.`);

    // TODO 실행 페이지 완성 후 이동
    // navigate("/execution");
  };

  const handleSkip = () => {
    const skip = window.confirm("이번 추천을 건너뛸까요?");

    if (!skip) {
      return;
    }

    console.log("이번 추천 건너뛰기");

    // TODO:
    // recommendation session
    // selection_status = SKIPPED 처리 API 연결

    navigate("/");
  };

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
            {/* 불안한 - 받은 사용자의 기분으로 변경 상황 앞에 사용자 상황 넣을지 말지*/}
            지금의 <strong>불안한 마음</strong>과 현재 상황을 고려해
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
                    {item.place.name} · {item.place.distance}
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
