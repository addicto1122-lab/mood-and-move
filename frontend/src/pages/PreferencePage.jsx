import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getHobbies, getPreferences, updatePreferences } from "../api/authApi";

import {
  ACTIVITY_STYLE_OPTIONS,
  ENVIRONMENT_OPTIONS,
  SOCIAL_OPTIONS,
  AVAILABLE_TIME_OPTIONS,
  DEFAULT_ACTIVITY_STYLE,
  DEFAULT_ENVIRONMENT,
  DEFAULT_SOCIAL,
  DEFAULT_AVAILABLE_TIME
} from "../constants/userOptions";

import "./PreferencePage.css";

export default function PreferencePage() {
  const navigate = useNavigate();

  // DB에서 받아오는 취미 목록
  const [hobbies, setHobbies] = useState([]);

  // 현재 사용자 선택값
  const [selectedHobbies, setSelectedHobbies] = useState([]);

  const [activityStyle, setActivityStyle] = useState(DEFAULT_ACTIVITY_STYLE);

  const [activityEnvironment, setActivityEnvironment] =
    useState(DEFAULT_ENVIRONMENT);

  const [socialPreference, setSocialPreference] = useState(DEFAULT_SOCIAL);

  const [defaultAvailableMinutes, setDefaultAvailableMinutes] = useState(
    DEFAULT_AVAILABLE_TIME
  );

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");
  const [toast, setToast] = useState("");

  /*
   * 페이지 진입 시
   *
   * 1. hobbies 테이블에서 전체 취미 조회
   * 2. 현재 사용자의 취미 및 선호 조회
   */
  useEffect(() => {
    async function fetchData() {
      try {
        const [hobbyData, preferenceData] = await Promise.all([
          getHobbies(),
          getPreferences()
        ]);

        // hobbies
        setHobbies(hobbyData ?? []);

        // user_hobbies
        setSelectedHobbies(preferenceData.hobbyIds ?? []);

        // user_preferences
        setActivityStyle(
          preferenceData.activityStyle ?? DEFAULT_ACTIVITY_STYLE
        );

        setActivityEnvironment(
          preferenceData.activityEnvironment ?? DEFAULT_ENVIRONMENT
        );

        setSocialPreference(preferenceData.socialPreference ?? DEFAULT_SOCIAL);

        /*
         * null = 상관없음
         *
         * 서버 응답에 값이 없을 때만
         * 기본값을 사용
         */
        if (preferenceData.defaultAvailableMinutes !== undefined) {
          setDefaultAvailableMinutes(preferenceData.defaultAvailableMinutes);
        } else {
          setDefaultAvailableMinutes(DEFAULT_AVAILABLE_TIME);
        }
      } catch (error) {
        console.error(error);

        setError(error.message || "취미 및 선호 설정을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, []);

  /*
   * 취미 선택 / 선택 해제
   */
  const toggleHobby = (id) => {
    setError("");

    setSelectedHobbies((prev) => {
      if (prev.includes(id)) {
        return prev.filter((hobbyId) => hobbyId !== id);
      }

      return [...prev, id];
    });
  };

  /*
   * 토스트
   */
  const showToast = (message) => {
    setToast(message);

    setTimeout(() => {
      setToast("");
    }, 2000);
  };

  /*
   * 저장
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    setError("");

    if (selectedHobbies.length === 0) {
      setError("좋아하는 활동을 하나 이상 선택해주세요.");

      return;
    }

    if (!activityStyle) {
      setError("활동 성향을 선택해주세요.");

      return;
    }

    if (!activityEnvironment) {
      setError("활동 환경을 선택해주세요.");

      return;
    }

    if (!socialPreference) {
      setError("활동 방식을 선택해주세요.");

      return;
    }

    try {
      setSaving(true);

      await updatePreferences({
        hobbyIds: selectedHobbies,
        activityStyle,
        activityEnvironment,
        socialPreference,
        defaultAvailableMinutes
      });

      showToast("취미 및 선호 설정이 저장되었습니다.");
    } catch (error) {
      console.error(error);

      setError(error.message || "선호 설정 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <main className="preference-page">
        <div className="preference-loading">불러오는 중...</div>
      </main>
    );
  }

  return (
    <main className="preference-page">
      <section className="preference-container">
        {/* HEADER */}
        <header className="preference-header">
          <button
            type="button"
            className="preference-back"
            onClick={() => navigate("/mypage")}
            aria-label="마이페이지로 돌아가기"
          >
            ‹
          </button>

          <div>
            <h1>취미 및 선호 설정</h1>

            <p>추천에 사용할 취미와 활동 선호를 변경할 수 있어요.</p>
          </div>
        </header>

        <form className="preference-form" onSubmit={handleSubmit}>
          {/* =========================
              취미
          ========================= */}

          <section className="preference-section">
            <div className="preference-section-header">
              <div>
                <h2>평소 어떤 걸 좋아하나요?</h2>

                <p>하나 이상 선택해주세요.</p>
              </div>

              <span>{selectedHobbies.length}개 선택</span>
            </div>

            {hobbies.length > 0 ? (
              <div className="preference-hobby-grid">
                {hobbies.map((hobby) => (
                  <button
                    key={hobby.id}
                    type="button"
                    className={
                      selectedHobbies.includes(hobby.id)
                        ? "preference-hobby-card selected"
                        : "preference-hobby-card"
                    }
                    onClick={() => toggleHobby(hobby.id)}
                  >
                    <strong>{hobby.name}</strong>
                  </button>
                ))}
              </div>
            ) : (
              <p className="preference-empty">등록된 취미가 없습니다.</p>
            )}
          </section>

          {/* =========================
              활동 성향
          ========================= */}

          <section className="preference-section">
            <div className="preference-section-header">
              <div>
                <h2>어떤 활동을 좋아하나요?</h2>

                <p>추천 행동의 분위기를 선택해주세요.</p>
              </div>
            </div>

            <div className="preference-style-list">
              {ACTIVITY_STYLE_OPTIONS.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={
                    activityStyle === item.value
                      ? "preference-style-card selected"
                      : "preference-style-card"
                  }
                  onClick={() => {
                    setActivityStyle(item.value);

                    setError("");
                  }}
                >
                  <span className="preference-style-emoji">{item.emoji}</span>

                  <div>
                    <strong>{item.label}</strong>

                    <p>{item.description}</p>
                  </div>

                  <span className="preference-style-check">
                    {activityStyle === item.value ? "✓" : ""}
                  </span>
                </button>
              ))}
            </div>
          </section>

          {/* =========================
              환경
          ========================= */}

          <section className="preference-section">
            <div className="preference-section-header">
              <div>
                <h2>어디가 더 편한가요?</h2>

                <p>평소 선호하는 활동 장소예요.</p>
              </div>
            </div>

            <div className="preference-choice-grid">
              {ENVIRONMENT_OPTIONS.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={
                    activityEnvironment === item.value
                      ? "preference-choice-card selected"
                      : "preference-choice-card"
                  }
                  onClick={() => {
                    setActivityEnvironment(item.value);

                    setError("");
                  }}
                >
                  <span>{item.emoji}</span>

                  <strong>{item.label}</strong>
                </button>
              ))}
            </div>
          </section>

          {/* =========================
              혼자 / 함께
          ========================= */}

          <section className="preference-section">
            <div className="preference-section-header">
              <div>
                <h2>누구와 하는 게 편한가요?</h2>

                <p>평소 선호하는 활동 방식을 선택해주세요.</p>
              </div>
            </div>

            <div className="preference-choice-grid">
              {SOCIAL_OPTIONS.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  className={
                    socialPreference === item.value
                      ? "preference-choice-card selected"
                      : "preference-choice-card"
                  }
                  onClick={() => {
                    setSocialPreference(item.value);

                    setError("");
                  }}
                >
                  <span>{item.emoji}</span>

                  <strong>{item.label}</strong>
                </button>
              ))}
            </div>
          </section>

          {/* =========================
              시간
          ========================= */}

          <section className="preference-section">
            <div className="preference-section-header">
              <div>
                <h2>평소 얼마나 시간을 낼 수 있나요?</h2>

                <p>행동을 추천할 때 참고할게요.</p>
              </div>
            </div>

            <div className="preference-time-grid">
              {AVAILABLE_TIME_OPTIONS.map((item) => (
                <button
                  key={item.label}
                  type="button"
                  className={
                    defaultAvailableMinutes === item.value
                      ? "preference-time-card selected"
                      : "preference-time-card"
                  }
                  onClick={() => {
                    setDefaultAvailableMinutes(item.value);

                    setError("");
                  }}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </section>

          {error && <div className="preference-error">{error}</div>}

          <button type="submit" className="preference-save" disabled={saving}>
            {saving ? "저장 중..." : "설정 저장"}
          </button>
        </form>
      </section>

      {toast && <div className="preference-toast">{toast}</div>}
    </main>
  );
}
