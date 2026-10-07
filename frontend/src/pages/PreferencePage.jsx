import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  getHobbies,
  getPreferences,
  updatePreferences,
  searchRegions
} from "../api/authApi";

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

  /*
   * 기본 활동 지역
   */
  const [selectedRegion, setSelectedRegion] = useState(null);
  const [regionEditing, setRegionEditing] = useState(true);

  const [regionQuery, setRegionQuery] = useState("");
  const [regionResults, setRegionResults] = useState([]);

  const [regionSearching, setRegionSearching] = useState(false);
  const [regionError, setRegionError] = useState("");

  /*
   * 공통 상태
   */
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");
  const [toast, setToast] = useState("");

  /*
   * 페이지 진입 시
   *
   * 1. 전체 취미 조회
   * 2. 현재 사용자의 취미 / 선호 / 기본 활동 지역 조회
   */
  useEffect(() => {
    async function fetchData() {
      try {
        const [hobbyData, preferenceData] = await Promise.all([
          getHobbies(),
          getPreferences()
        ]);

        /*
         * hobbies
         */
        setHobbies(hobbyData ?? []);

        /*
         * user_hobbies
         */
        setSelectedHobbies(preferenceData.hobbyIds ?? []);

        /*
         * user_preferences
         */
        setActivityStyle(
          preferenceData.activityStyle ?? DEFAULT_ACTIVITY_STYLE
        );

        setActivityEnvironment(
          preferenceData.activityEnvironment ?? DEFAULT_ENVIRONMENT
        );

        setSocialPreference(preferenceData.socialPreference ?? DEFAULT_SOCIAL);

        /*
         * null = 상관없음
         */
        if (preferenceData.defaultAvailableMinutes !== undefined) {
          setDefaultAvailableMinutes(preferenceData.defaultAvailableMinutes);
        } else {
          setDefaultAvailableMinutes(DEFAULT_AVAILABLE_TIME);
        }

        /*
         * 기본 활동 지역
         *
         * regionCode가 없어도
         * 지역명이 있으면 기존 지역으로 표시
         */
        if (preferenceData.defaultRegionName) {
          setSelectedRegion({
            regionName: preferenceData.defaultRegionName,

            regionCode: preferenceData.defaultRegionCode ?? null,

            latitude: preferenceData.defaultRegionLatitude ?? null,

            longitude: preferenceData.defaultRegionLongitude ?? null
          });

          setRegionEditing(false);
        } else {
          setSelectedRegion(null);
          setRegionEditing(true);
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
   * 기본 활동 지역 검색
   */
  const handleRegionSearch = async () => {
    const query = regionQuery.trim();

    if (!query) {
      setRegionError("검색할 동네를 입력해주세요.");

      setRegionResults([]);

      return;
    }

    try {
      setRegionSearching(true);
      setRegionError("");

      const data = await searchRegions(query);

      setRegionResults(data ?? []);

      if (!data || data.length === 0) {
        setRegionError("검색 결과가 없습니다.");
      }
    } catch (error) {
      console.error(error);

      setRegionResults([]);

      setRegionError(error.message || "지역 검색에 실패했습니다.");
    } finally {
      setRegionSearching(false);
    }
  };

  /*
   * 지역 선택
   */
  const handleRegionSelect = (region) => {
    setSelectedRegion(region);

    setRegionQuery("");
    setRegionResults([]);
    setRegionError("");

    setRegionEditing(false);

    setError("");
  };

  /*
   * 지역 변경
   */
  const handleRegionChange = () => {
    setRegionEditing(true);

    setRegionQuery("");
    setRegionResults([]);
    setRegionError("");
  };

  /*
   * 지역 변경 취소
   */
  const handleRegionCancel = () => {
    setRegionEditing(false);

    setRegionQuery("");
    setRegionResults([]);
    setRegionError("");
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
        defaultAvailableMinutes,

        defaultRegionName: selectedRegion?.regionName ?? null,

        defaultRegionCode: selectedRegion?.regionCode ?? null,

        defaultRegionLatitude: selectedRegion?.latitude ?? null,

        defaultRegionLongitude: selectedRegion?.longitude ?? null
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

          {/* =========================
              기본 활동 지역
          ========================= */}

          <section className="preference-section">
            <div className="preference-section-header">
              <div>
                <h2>기본 활동 지역</h2>

                <p>주변 장소 추천의 기준이 되는 동네예요.</p>
              </div>

              <span>선택</span>
            </div>

            {!regionEditing && selectedRegion ? (
              <div className="preference-region-selected">
                <div className="preference-region-selected-info">
                  <span className="preference-region-icon">📍</span>

                  <div>
                    <span>현재 기본 활동 지역</span>

                    <strong>{selectedRegion.regionName}</strong>
                  </div>
                </div>

                <button
                  type="button"
                  className="preference-region-change"
                  onClick={handleRegionChange}
                >
                  변경
                </button>
              </div>
            ) : (
              <>
                <div className="preference-region-search">
                  <input
                    type="text"
                    value={regionQuery}
                    placeholder="예: 인계동"
                    onChange={(e) => {
                      setRegionQuery(e.target.value);

                      setRegionError("");
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();

                        handleRegionSearch();
                      }
                    }}
                  />

                  <button
                    type="button"
                    onClick={handleRegionSearch}
                    disabled={regionSearching}
                  >
                    {regionSearching ? "검색 중..." : "검색"}
                  </button>
                </div>

                {selectedRegion && (
                  <button
                    type="button"
                    className="preference-region-cancel"
                    onClick={handleRegionCancel}
                  >
                    변경 취소
                  </button>
                )}

                {regionError && (
                  <p className="preference-region-error">{regionError}</p>
                )}

                {regionResults.length > 0 && (
                  <div className="preference-region-results">
                    {regionResults.map((region) => (
                      <button
                        key={`${region.regionCode ?? "none"}-${region.latitude}-${region.longitude}`}
                        type="button"
                        className="preference-region-result"
                        onClick={() => handleRegionSelect(region)}
                      >
                        <span>📍</span>

                        <div>
                          <strong>{region.regionName}</strong>

                          <p>기본 활동 지역으로 설정</p>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}
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
