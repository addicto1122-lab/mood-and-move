import { useNavigate } from "react-router-dom";
import "./HomePage.css";

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <main className="home-page">
      <header className="home-header">
        <div>
          <div className="brand">
            Mood<span>&</span>Move
          </div>
          <p>좋은 오후예요, 민지님</p>
        </div>

        <button className="avatar" onClick={() => navigate("/mypage")}>
          민
        </button>
      </header>

      <section className="mood-hero">
        <div className="orb orb-one" />
        <div className="orb orb-two" />

        <div className="hero-content">
          <span className="eyebrow">TODAY · 10월 1일</span>

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
