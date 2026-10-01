import { useState } from "react";
import "./MoodWritePage.css";

const emotions = [
  { code: "ANGRY", name: "화남", emoji: "😡" },
  { code: "ANXIOUS", name: "불안", emoji: "😰" },
  { code: "SAD", name: "슬픔", emoji: "😢" },
  { code: "NEUTRAL", name: "보통", emoji: "😐" },
  { code: "CALM", name: "편안함", emoji: "🙂" },
  { code: "JOY", name: "기쁨", emoji: "😄" }
];

export default function MoodWritePage() {
  const [emotion, setEmotion] = useState(null);
  const [intensity, setIntensity] = useState(5);
  const [currentActivity, setCurrentActivity] = useState("");
  const [diary, setDiary] = useState("");

  return (
    <main className="mood-page">
      <header className="page-header">
        <span>오늘의 기록</span>
        <h1>지금 마음은 어떤가요?</h1>
        <p>정답은 없어요. 지금 느끼는 그대로 남겨주세요.</p>
      </header>

      <section className="form-section">
        <h2>감정 선택</h2>

        <div className="emotion-grid">
          {emotions.map((item) => (
            <button
              key={item.code}
              className={
                emotion === item.code
                  ? "emotion-button selected"
                  : "emotion-button"
              }
              onClick={() => setEmotion(item.code)}
            >
              <span>{item.emoji}</span>
              {item.name}
            </button>
          ))}
        </div>
      </section>

      <section className="form-section">
        <h2>감정의 강도</h2>

        <div className="intensity-scale">
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((score) => (
            <button
              key={score}
              className={intensity === score ? "selected" : ""}
              onClick={() => setIntensity(score)}
            >
              {score}
            </button>
          ))}
        </div>
      </section>

      <section className="form-section">
        <h2>지금 무엇을 하고 있나요?</h2>

        <input
          className="activity-input"
          type="text"
          value={currentActivity}
          onChange={(e) => setCurrentActivity(e.target.value)}
          placeholder="예: 프로젝트 작업 중, 집에서 쉬는 중"
          maxLength={100}
        />
      </section>

      <section className="form-section">
        <h2>오늘의 마음을 기록해볼까요?</h2>

        <textarea
          value={diary}
          onChange={(e) => setDiary(e.target.value)}
          placeholder="오늘 있었던 일이나 지금 드는 생각을 자유롭게 적어주세요."
          maxLength={500}
        />

        <span className="text-count">{diary.length} / 500</span>
      </section>

      <button className="save-mood-button">오늘의 감정 저장하기</button>
    </main>
  );
}
