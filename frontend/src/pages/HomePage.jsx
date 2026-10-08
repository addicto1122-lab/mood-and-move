import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getMe } from "../api/authApi";

import { getHome } from "../api/homeApi";

import "./HomePage.css";

export default function HomePage() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [homeData, setHomeData] = useState({
    latestMood: null,
    todayRecommendation: null
  });

  const [now, setNow] = useState(new Date());

  /*
   * 사용자 및 홈 정보 조회
   */
  useEffect(() => {
    async function fetchHome() {
      try {
        const [userData, homeResponse] = await Promise.all([
          getMe(),
          getHome()
        ]);

        setUser(userData);
        setHomeData(homeResponse);
      } catch (error) {
        console.error(error);
      }
    }

    fetchHome();
  }, []);

  /*
   * 현재 시간 갱신
   */
  useEffect(() => {
    const timer = setInterval(() => {
      setNow(new Date());
    }, 60 * 1000);

    return () => {
      clearInterval(timer);
    };
  }, []);

  /*
   * 시간대별 인사말
   */
  const getGreeting = () => {
    const hour = now.getHours();

    if (hour < 6) {
      return "조용한 새벽이에요";
    }

    if (hour < 12) {
      return "좋은 아침이에요";
    }

    if (hour < 18) {
      return "편안한 오후예요";
    }

    return "포근한 저녁이에요";
  };

  /*
   * 날짜를 YYYY-MM-DD 형태로 변환
   */
  const getDateKey = (date) => {
    const year = date.getFullYear();

    const month = String(date.getMonth() + 1).padStart(2, "0");

    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
  };

  /*
   * 최근 감정 날짜 표시
   */
  const getMoodDateText = (entryDate) => {
    if (!entryDate) {
      return "";
    }

    const today = new Date(now);

    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);

    if (entryDate === getDateKey(today)) {
      return "오늘";
    }

    if (entryDate === getDateKey(yesterday)) {
      return "어제";
    }

    const [, month, day] = entryDate.split("-");

    return `${Number(month)}월 ${Number(day)}일`;
  };

  const todayText = `${now.getMonth() + 1}월 ${now.getDate()}일`;

  const latestMood = homeData.latestMood;

  const todayRecommendation = homeData.todayRecommendation;

  const handleMoodClick = () => {
    const today = getDateKey(now);

    if (latestMood?.entryDate === today) {
      alert("오늘의 일기는 이미 작성했습니다.");
      return;
    }

    navigate("/mood");
  };

  return (
    <main className="home-page">
      <header className="home-header">
        <div>
          <div className="brand">
            Mood<span>&</span>Move
          </div>

          <p>
            {getGreeting()}, {user?.nickname ?? "사용자"}님
          </p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={handleMoodClick}
        >
          {user?.nickname?.charAt(0) ?? "M"}
        </button>
      </header>

      <section className="mood-hero">
        <div className="orb orb-one" />
        <div className="orb orb-two" />

        <div className="hero-content">
          <span className="eyebrow">TODAY · {todayText}</span>

          <h1>
            오늘 기분은
            <br />
            어떤가요?
          </h1>

          <p>지금의 마음을 가볍게 남겨보세요.</p>

          <button
            type="button"
            className="primary-button"
            onClick={() => navigate("/mood")}
          >
            감정 기록하기
            <span>›</span>
          </button>
        </div>
      </section>

      <div className="section-title">
        <h2>최근 마음</h2>

        <button type="button" onClick={() => navigate("/stats")}>
          전체 보기
        </button>
      </div>

      {latestMood ? (
        <section className="card recent-card">
          <div className="emotion-stamp">{latestMood.emoji ?? "✨"}</div>

          <div className="recent-copy">
            <strong>
              {getMoodDateText(latestMood.entryDate)}
              {" · "}
              {latestMood.emotionName}
            </strong>

            <span>
              기분 점수 {latestMood.moodScore}
              {" · "}
              강도 {latestMood.intensity}
            </span>
          </div>

          <div
            className="mini-bars"
            aria-label={`감정 강도 ${latestMood.intensity}`}
          >
            {[1, 2, 3, 4, 5].map((level) => (
              <i
                key={level}
                className={
                  latestMood.intensity >= level * 2 - 1 ? "active" : ""
                }
              />
            ))}
          </div>
        </section>
      ) : (
        <section className="card recent-card">
          <div className="emotion-stamp">✨</div>

          <div className="recent-copy">
            <strong>아직 기록된 마음이 없어요</strong>

            <span>오늘의 마음을 처음 남겨보세요.</span>
          </div>
        </section>
      )}

      {todayRecommendation && (
        <>
          <div className="section-title">
            <h2>지금 필요한 한 걸음</h2>
          </div>

          <section className="card action-card">
            <div className="action-icon">🌿</div>

            <div className="action-copy">
              <span>오늘의 추천</span>

              <h3>
                {todayRecommendation.durationMinutes
                  ? `${todayRecommendation.durationMinutes}분 `
                  : ""}
                {todayRecommendation.actionName}
              </h3>

              <p>{todayRecommendation.reason}</p>
            </div>

            <button
              type="button"
              className="round-arrow"
              onClick={() =>
                navigate("/recommendation", {
                  state: {
                    moodEntryId: latestMood?.moodEntryId
                  }
                })
              }
              aria-label="오늘의 추천 보기"
            >
              ›
            </button>
          </section>

          <p className="tiny-note">
            ✧ 오늘의 기록을 남기면 더 잘 맞는 행동을 추천해요.
          </p>
        </>
      )}
    </main>
  );
}
