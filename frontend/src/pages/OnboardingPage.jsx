import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getHobbies,
  completeOnboarding,
  updatePreferences
} from "../api/authApi";

import {
  AGE_GROUP_OPTIONS,
  GENDER_OPTIONS,
  ACTIVITY_STYLE_OPTIONS,
  ENVIRONMENT_OPTIONS,
  SOCIAL_OPTIONS,
  AVAILABLE_TIME_OPTIONS,
  DEFAULT_ACTIVITY_STYLE,
  DEFAULT_ENVIRONMENT,
  DEFAULT_SOCIAL,
  DEFAULT_AVAILABLE_TIME
} from "../constants/userOptions";

import "./OnboardingPage.css";

export default function OnboardingPage() {
  const navigate = useNavigate();

  /*
   * DB에서 받아오는 취미 목록
   */
  const [hobbies, setHobbies] = useState([]);

  /*
   * 1 = 필수 정보
   * 2 = 선택 정보
   */
  const [step, setStep] = useState(1);

  /*
   * STEP 1
   */
  const [ageGroup, setAgeGroup] = useState("");
  const [gender, setGender] = useState("");
  const [selectedHobbies, setSelectedHobbies] = useState([]);

  /*
   * STEP 2
   */
  const [activityStyle, setActivityStyle] = useState(DEFAULT_ACTIVITY_STYLE);

  const [activityEnvironment, setActivityEnvironment] =
    useState(DEFAULT_ENVIRONMENT);

  const [socialPreference, setSocialPreference] = useState(DEFAULT_SOCIAL);

  const [defaultAvailableMinutes, setDefaultAvailableMinutes] = useState(
    DEFAULT_AVAILABLE_TIME
  );

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  /*
   * 취미 목록 조회
   */
  useEffect(() => {
    async function fetchHobbies() {
      try {
        const data = await getHobbies();

        setHobbies(data ?? []);
      } catch (error) {
        console.error(error);

        setLoadError(error.message || "취미 정보를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }

    fetchHobbies();
  }, []);

  /*
   * 취미 선택 / 선택 해제
   */
  const toggleHobby = (id) => {
    setSelectedHobbies((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  /*
   * STEP 1
   * 필수 정보 저장
   */
  const handleRequiredSubmit = async (e) => {
    e.preventDefault();

    if (!ageGroup) {
      alert("나이대를 선택해주세요.");
      return;
    }

    if (!gender) {
      alert("성별을 선택해주세요.");
      return;
    }

    if (selectedHobbies.length === 0) {
      alert("좋아하는 활동을 하나 이상 선택해주세요.");
      return;
    }

    try {
      setIsSubmitting(true);

      await completeOnboarding({
        ageGroup,
        gender,
        hobbyIds: selectedHobbies
      });

      /*
       * 필수 온보딩 완료
       */
      setStep(2);

      window.scrollTo({
        top: 0,
        behavior: "smooth"
      });
    } catch (error) {
      console.error(error);

      alert(error.message || "필수 정보 저장에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  /*
   * STEP 2
   * 선택 정보 저장
   */
  const handlePreferenceSubmit = async (e) => {
    e.preventDefault();

    try {
      setIsSubmitting(true);

      await updatePreferences({
        hobbyIds: selectedHobbies,
        activityStyle,
        activityEnvironment,
        socialPreference,
        defaultAvailableMinutes
      });

      navigate("/", {
        replace: true
      });
    } catch (error) {
      console.error(error);

      alert(error.message || "선호 설정 저장에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  /*
   * 선택 설정 건너뛰기
   */
  const handleSkip = () => {
    navigate("/", {
      replace: true
    });
  };

  if (loading) {
    return (
      <main className="onboarding-page">
        <div className="onboarding-container">
          <div className="onboarding-loading">불러오는 중...</div>
        </div>
      </main>
    );
  }

  if (loadError) {
    return (
      <main className="onboarding-page">
        <div className="onboarding-container">
          <div className="onboarding-load-error">
            <p>{loadError}</p>

            <button
              type="button"
              className="onboarding-submit"
              onClick={() => window.location.reload()}
            >
              다시 불러오기
            </button>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="onboarding-page">
      <div className="onboarding-container">
        <header className="onboarding-header">
          <div className="onboarding-logo">
            Mood<span>&</span>Move
          </div>

          <div className="onboarding-step-badge">{step} / 2</div>

          {step === 1 ? (
            <>
              <span className="onboarding-eyebrow">
                나에게 맞는 추천을 위해
              </span>

              <h1>
                기본 정보를
                <br />
                알려주세요
              </h1>

              <p>
                추천에 필요한 기본 정보예요.
                <br />세 항목을 모두 선택해주세요.
              </p>
            </>
          ) : (
            <>
              <span className="onboarding-eyebrow">선택 설정</span>

              <h1>
                조금 더 알려주면
                <br />
                추천이 더 잘 맞아요
              </h1>

              <p>
                아래 설정은 선택사항이에요.
                <br />
                지금 설정하거나 나중에 변경할 수 있어요.
              </p>
            </>
          )}
        </header>

        {/* =========================
            STEP 1
        ========================= */}

        {step === 1 && (
          <form className="onboarding-form" onSubmit={handleRequiredSubmit}>
            {/* 나이대 */}
            <section className="onboarding-section">
              <div className="onboarding-section-title">
                <div>
                  <h2>나이대를 알려주세요</h2>

                  <p>추천 내용을 조정할 때 참고할게요.</p>
                </div>

                <span>필수</span>
              </div>

              <div className="age-grid">
                {AGE_GROUP_OPTIONS.map((item) => (
                  <button
                    key={item.value}
                    type="button"
                    className={
                      ageGroup === item.value
                        ? "time-card selected"
                        : "time-card"
                    }
                    onClick={() => setAgeGroup(item.value)}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </section>

            {/* 성별 */}
            <section className="onboarding-section">
              <div className="onboarding-section-title">
                <div>
                  <h2>성별을 알려주세요</h2>

                  <p>원하지 않는 경우 응답하지 않음을 선택할 수 있어요.</p>
                </div>

                <span>필수</span>
              </div>

              <div className="gender-grid">
                {GENDER_OPTIONS.map((item) => (
                  <button
                    key={item.value}
                    type="button"
                    className={
                      gender === item.value
                        ? "choice-card selected"
                        : "choice-card"
                    }
                    onClick={() => setGender(item.value)}
                  >
                    {item.emoji && <span>{item.emoji}</span>}

                    <strong>{item.label}</strong>
                  </button>
                ))}
              </div>
            </section>

            {/* 취미 */}
            <section className="onboarding-section">
              <div className="onboarding-section-title">
                <div>
                  <h2>평소 어떤 걸 좋아하나요?</h2>

                  <p>여러 개 선택할 수 있어요.</p>
                </div>

                <span>
                  {selectedHobbies.length === 0
                    ? "필수"
                    : `${selectedHobbies.length}개 선택`}
                </span>
              </div>

              {hobbies.length > 0 ? (
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
                      <strong>{hobby.name}</strong>
                    </button>
                  ))}
                </div>
              ) : (
                <p className="onboarding-empty">등록된 취미가 없습니다.</p>
              )}
            </section>

            <button
              type="submit"
              className="onboarding-submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? "저장 중..." : "완료"}
            </button>
          </form>
        )}

        {/* =========================
            STEP 2
        ========================= */}

        {step === 2 && (
          <form className="onboarding-form" onSubmit={handlePreferenceSubmit}>
            {/* 활동 성향 */}
            <section className="onboarding-section">
              <div className="onboarding-section-title">
                <div>
                  <h2>어떤 활동을 좋아하나요?</h2>

                  <p>추천 행동의 분위기를 선택해주세요.</p>
                </div>
              </div>

              <div className="preference-list">
                {ACTIVITY_STYLE_OPTIONS.map((item) => (
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
                      <strong>{item.label}</strong>

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
                {ENVIRONMENT_OPTIONS.map((item) => (
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

                    <strong>{item.label}</strong>
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
                {SOCIAL_OPTIONS.map((item) => (
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

                    <strong>{item.label}</strong>
                  </button>
                ))}
              </div>
            </section>

            {/* 활동 가능 시간 */}
            <section className="onboarding-section">
              <div className="onboarding-section-title">
                <div>
                  <h2>평소 얼마나 시간을 낼 수 있나요?</h2>

                  <p>행동을 추천할 때 참고할게요.</p>
                </div>
              </div>

              <div className="time-grid">
                {AVAILABLE_TIME_OPTIONS.map((item) => (
                  <button
                    key={item.label}
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

            <div className="onboarding-actions">
              <button
                type="submit"
                className="onboarding-submit"
                disabled={isSubmitting}
              >
                {isSubmitting ? "저장 중..." : "설정하고 시작하기"}
              </button>

              <button
                type="button"
                className="onboarding-skip"
                onClick={handleSkip}
                disabled={isSubmitting}
              >
                나중에 할게요
              </button>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}
