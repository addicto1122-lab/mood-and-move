import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./OnboardingPage.css";

/*
 * TODO
 * 백엔드 연결 후에는 /api/hobbies 등에서 목록을 받아오는 방식으로 변경
 *
 * 현재 V2 seed 기준
 * 1 산책
 * 2 독서
 * 3 음악
 * 4 게임
 * 5 운동
 */
const hobbies = [
  { id: 1, name: "산책", emoji: "🚶" },
  { id: 2, name: "독서", emoji: "📚" },
  { id: 3, name: "음악", emoji: "🎧" },
  { id: 4, name: "게임", emoji: "🎮" },
  { id: 5, name: "운동", emoji: "🏃" }
];

const activityStyles = [
  {
    value: "ACTIVE",
    title: "활동적인 편",
    description: "몸을 움직이는 활동이 좋아요.",
    emoji: "🏃"
  },
  {
    value: "CALM",
    title: "차분한 편",
    description: "편하게 쉬거나 집중하는 활동이 좋아요.",
    emoji: "🌿"
  },
  {
    value: "ANY",
    title: "상관없어요",
    description: "상황에 맞게 추천받고 싶어요.",
    emoji: "✨"
  }
];

const environments = [
  {
    value: "INDOOR",
    title: "실내",
    emoji: "🏠"
  },
  {
    value: "OUTDOOR",
    title: "실외",
    emoji: "🌳"
  },
  {
    value: "ANY",
    title: "상관없어요",
    emoji: "✨"
  }
];

const socialTypes = [
  {
    value: "ALONE",
    title: "혼자",
    emoji: "🙂"
  },
  {
    value: "SOCIAL",
    title: "함께",
    emoji: "👥"
  },
  {
    value: "ANY",
    title: "상관없어요",
    emoji: "✨"
  }
];

const availableTimes = [
  { value: "5", label: "5분" },
  { value: "10", label: "10분" },
  { value: "15", label: "15분" },
  { value: "30", label: "30분" },
  { value: "ANY", label: "상관없어요" }
];

export default function OnboardingPage() {
  const navigate = useNavigate();

  const [selectedHobbies, setSelectedHobbies] = useState([]);

  const [activityStyle, setActivityStyle] = useState("ANY");

  const [activityEnvironment, setActivityEnvironment] = useState("ANY");

  const [socialPreference, setSocialPreference] = useState("ANY");

  const [defaultAvailableMinutes, setDefaultAvailableMinutes] = useState("ANY");

  const toggleHobby = (id) => {
    setSelectedHobbies((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (selectedHobbies.length === 0) {
      alert("관심 있는 취미를 하나 이상 선택해주세요.");
      return;
    }

    const onboardingData = {
      hobbyIds: selectedHobbies,

      activityStyle,

      activityEnvironment,

      socialPreference,

      defaultAvailableMinutes:
        defaultAvailableMinutes === "ANY"
          ? null
          : Number(defaultAvailableMinutes)
    };

    console.log(onboardingData);

    /*
     * TODO 백엔드 API 연결
     *
     * 예시
     *
     * await fetch("/api/users/onboarding", {
     *   method: "POST",
     *   headers: {
     *     "Content-Type": "application/json",
     *   },
     *   credentials: "include",
     *   body: JSON.stringify(onboardingData),
     * });
     */

    navigate("/");
  };

  return (
    <main className="onboarding-page">
      <div className="onboarding-container">
        <header className="onboarding-header">
          <div className="onboarding-logo">
            Mood<span>&</span>Move
          </div>

          <span className="onboarding-eyebrow">나에게 맞는 추천을 위해</span>

          <h1>
            조금만 더
            <br />
            알려주세요
          </h1>

          <p>
            취향을 알려주면 지금의 기분과 상황에
            <br />더 잘 맞는 행동을 추천할게요.
          </p>
        </header>

        <form className="onboarding-form" onSubmit={handleSubmit}>
          {/* 취미 */}
          <section className="onboarding-section">
            <div className="onboarding-section-title">
              <div>
                <h2>평소 어떤 걸 좋아하나요?</h2>
                <p>여러 개 선택할 수 있어요.</p>
              </div>

              <span>{selectedHobbies.length}개 선택</span>
            </div>

            <div className="hobby-grid">
              {hobbies.map((hobby) => (
                <button
                  key={hobby.id}
                  type="button"
                  className={
                    selectedHobbies.includes(hobby.id)
                      ? "hobby-card selected"
                      : "hobby-card"
                  }
                  onClick={() => toggleHobby(hobby.id)}
                >
                  <span className="hobby-emoji">{hobby.emoji}</span>

                  <strong>{hobby.name}</strong>
                </button>
              ))}
            </div>
          </section>

          {/* 활동 성향 */}
          <section className="onboarding-section">
            <div className="onboarding-section-title">
              <div>
                <h2>어떤 활동을 좋아하나요?</h2>
                <p>추천 행동의 분위기를 선택해주세요.</p>
              </div>
            </div>

            <div className="preference-list">
              {activityStyles.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={
                    activityStyle === item.value
                      ? "preference-card selected"
                      : "preference-card"
                  }
                  onClick={() => setActivityStyle(item.value)}
                >
                  <span className="preference-emoji">{item.emoji}</span>

                  <div>
                    <strong>{item.title}</strong>
                    <p>{item.description}</p>
                  </div>

                  <span className="preference-check">
                    {activityStyle === item.value ? "✓" : ""}
                  </span>
                </button>
              ))}
            </div>
          </section>

          {/* 활동 환경 */}
          <section className="onboarding-section">
            <div className="onboarding-section-title">
              <div>
                <h2>어디가 더 편한가요?</h2>
                <p>평소 선호하는 활동 장소를 선택해주세요.</p>
              </div>
            </div>

            <div className="choice-grid">
              {environments.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={
                    activityEnvironment === item.value
                      ? "choice-card selected"
                      : "choice-card"
                  }
                  onClick={() => setActivityEnvironment(item.value)}
                >
                  <span>{item.emoji}</span>
                  <strong>{item.title}</strong>
                </button>
              ))}
            </div>
          </section>

          {/* 사회적 선호 */}
          <section className="onboarding-section">
            <div className="onboarding-section-title">
              <div>
                <h2>누구와 하는 게 편한가요?</h2>

                <p>평소 선호하는 활동 방식을 선택해주세요.</p>
              </div>
            </div>

            <div className="choice-grid">
              {socialTypes.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={
                    socialPreference === item.value
                      ? "choice-card selected"
                      : "choice-card"
                  }
                  onClick={() => setSocialPreference(item.value)}
                >
                  <span>{item.emoji}</span>
                  <strong>{item.title}</strong>
                </button>
              ))}
            </div>
          </section>

          {/* 기본 활동 가능 시간 */}
          <section className="onboarding-section">
            <div className="onboarding-section-title">
              <div>
                <h2>평소 얼마나 시간을 낼 수 있나요?</h2>

                <p>행동을 추천할 때 참고할게요.</p>
              </div>
            </div>

            <div className="time-grid">
              {availableTimes.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={
                    defaultAvailableMinutes === item.value
                      ? "time-card selected"
                      : "time-card"
                  }
                  onClick={() => setDefaultAvailableMinutes(item.value)}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </section>

          <button type="submit" className="onboarding-submit">
            Mood&Move 시작하기
          </button>
        </form>
      </div>
    </main>
  );
}
