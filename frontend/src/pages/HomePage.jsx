import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getMe } from "../api/authApi";

import "./HomePage.css";

export default function HomePage() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [now, setNow] = useState(new Date());

  /*
   * 사용자 정보 조회
   */
  useEffect(() => {
    async function fetchUser() {
      try {
        const data = await getMe();

        setUser(data);
      } catch (error) {
        console.error(error);
      }
    }

    fetchUser();
  }, []);

  /*
   * 현재 시간 갱신
   * 페이지를 오래 켜놔도 날짜/시간대가 바뀌도록 처리
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
      return "감성넘치는 새벽이에요";
    }

    if (hour < 12) {
      return "좋은 오전이에요";
    }

    return "좋은 오후예요";
  };

  /*
   * 오늘 날짜
   */
  const todayText = `${now.getMonth() + 1}월 ${now.getDate()}일`;

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
          className="avatar"
          onClick={() => navigate("/mypage")}
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

          <button className="primary-button" onClick={() => navigate("/mood")}>
            감정 기록하기
            <span>›</span>
          </button>
        </div>
      </section>

      <div className="section-title">
        <h2>최근 마음</h2>

        <button onClick={() => navigate("/stats")}>전체 보기</button>
      </div>

      <section className="card recent-card">
        <div className="emotion-stamp">🙂</div>

        <div className="recent-copy">
          <strong>어제는 편안했어요</strong>

          <span>기분 4 · 강도 3</span>
        </div>

        <div className="mini-bars">
          <i />
          <i />
          <i />
          <i />
          <i />
        </div>
      </section>

      <div className="section-title">
        <h2>지금 필요한 한 걸음</h2>
      </div>

      <section className="card action-card">
        <div className="action-icon">🌿</div>

        <div className="action-copy">
          <span>오늘의 추천</span>

          <h3>10분 동네 산책</h3>

          <p>잠깐 바람을 쐬며 머리를 환기해요.</p>
        </div>

        <button
          className="round-arrow"
          onClick={() => navigate("/recommendation")}
        >
          ›
        </button>
      </section>

      <p className="tiny-note">
        ✨ 오늘의 기록을 남기면 더 잘 맞는 행동을 추천해요.
      </p>
    </main>
  );
}
