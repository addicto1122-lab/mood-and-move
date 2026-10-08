import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { EMOTION_OPTIONS } from "../constants/moodOptions";
import {
  getPreferences,
  getLocationConsent,
  updateLocationConsent,
  searchRegions,
  updatePreferences
} from "../api/authApi";
import { createMood } from "../api/moodApi";
import "./MoodWritePage.css";

export default function MoodWritePage() {
  const navigate = useNavigate();

  const [emotion, setEmotion] = useState(null);
  const [intensity, setIntensity] = useState(5);
  const [currentActivity, setCurrentActivity] = useState("");
  const [diary, setDiary] = useState("");

  const [saving, setSaving] = useState(false);

  /*
   * 사용자 선호 설정
   */
  const [preferences, setPreferences] = useState(null);

  /*
   * 기본 활동 지역
   */
  const [selectedRegion, setSelectedRegion] = useState(null);

  const [regionEditing, setRegionEditing] = useState(false);

  const [regionQuery, setRegionQuery] = useState("");
  const [regionResults, setRegionResults] = useState([]);

  const [regionSearching, setRegionSearching] = useState(false);
  const [regionSaving, setRegionSaving] = useState(false);

  const [regionError, setRegionError] = useState("");

  /*
   * 추천 위치 방식
   *
   * SAVED   = 기본 활동 지역
   * CURRENT = 현재 위치
   * NONE    = 위치 사용 안 함
   */
  const [locationMode, setLocationMode] = useState(null);

  /*
   * 현재 위치 좌표
   *
   * DB에 저장하지 않고
   * 이번 추천 요청에서만 사용
   */
  const [currentLocation, setCurrentLocation] = useState(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState("");

  /*
   * 위치 약관
   */
  const [locationConsent, setLocationConsent] = useState(null);
  const [consentModalOpen, setConsentModalOpen] = useState(false);
  const [consentSaving, setConsentSaving] = useState(false);

  /*
   * 페이지 초기 데이터
   */
  useEffect(() => {
    async function fetchInitialData() {
      try {
        const [preferenceData, consentData] = await Promise.all([
          getPreferences(),
          getLocationConsent()
        ]);

        setPreferences(preferenceData);
        setLocationConsent(consentData);

        if (preferenceData.defaultRegionName) {
          setSelectedRegion({
            regionName: preferenceData.defaultRegionName,
            regionCode: preferenceData.defaultRegionCode ?? null,
            latitude: preferenceData.defaultRegionLatitude ?? null,
            longitude: preferenceData.defaultRegionLongitude ?? null
          });
        }
      } catch (error) {
        console.error(error);
      }
    }

    fetchInitialData();
  }, []);

  /*
   * 지역 검색
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
   * 기본 활동 지역 변경
   *
   * 현재 사용자 선호 정보는 유지하고
   * 주소만 변경
   */
  const handleRegionSelect = async (region) => {
    if (!preferences) {
      setRegionError("사용자 설정을 불러오지 못했습니다.");
      return;
    }

    try {
      setRegionSaving(true);
      setRegionError("");

      await updatePreferences({
        hobbyIds: preferences.hobbyIds,
        activityStyle: preferences.activityStyle,
        activityEnvironment: preferences.activityEnvironment,
        socialPreference: preferences.socialPreference,
        defaultAvailableMinutes: preferences.defaultAvailableMinutes,

        defaultRegionName: region.regionName,
        defaultRegionCode: region.regionCode,
        defaultRegionLatitude: region.latitude,
        defaultRegionLongitude: region.longitude
      });

      setSelectedRegion(region);

      /*
       * 로컬 preferences에도 반영
       */
      setPreferences((prev) => ({
        ...prev,

        defaultRegionName: region.regionName,
        defaultRegionCode: region.regionCode,
        defaultRegionLatitude: region.latitude,
        defaultRegionLongitude: region.longitude
      }));

      setLocationMode("SAVED");

      setRegionQuery("");
      setRegionResults([]);
      setRegionEditing(false);
    } catch (error) {
      console.error(error);

      setRegionError(error.message || "기본 활동 지역 변경에 실패했습니다.");
    } finally {
      setRegionSaving(false);
    }
  };

  /*
   * 브라우저 현재 위치 요청
   */
  const requestCurrentLocation = () => {
    setLocationError("");

    if (!navigator.geolocation) {
      setLocationError("현재 브라우저에서는 위치 정보를 사용할 수 없습니다.");
      return;
    }

    setLocationLoading(true);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const latitude = position.coords.latitude;
        const longitude = position.coords.longitude;

        setCurrentLocation({
          latitude,
          longitude
        });

        setLocationMode("CURRENT");
        setLocationLoading(false);
      },

      (error) => {
        console.error(error);

        setCurrentLocation(null);
        setLocationMode(null);

        if (error.code === 1) {
          setLocationError("브라우저의 위치 권한이 거부되었습니다.");
        } else {
          setLocationError("현재 위치를 확인하지 못했습니다.");
        }

        setLocationLoading(false);
      },

      {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 60000
      }
    );
  };

  /*
   * 현재 위치 버튼 클릭
   */
  const handleCurrentLocationClick = () => {
    setLocationError("");

    /*
     * 서비스 위치 약관 미동의
     */
    if (!locationConsent?.agreed) {
      setConsentModalOpen(true);
      return;
    }

    /*
     * 이미 서비스 약관 동의 상태
     */
    requestCurrentLocation();
  };

  /*
   * 현재 위치 약관 동의
   */
  const handleConsentAgree = async () => {
    try {
      setConsentSaving(true);

      await updateLocationConsent(true);

      setLocationConsent((prev) => ({
        ...prev,
        agreed: true
      }));

      setConsentModalOpen(false);

      /*
       * 서비스 약관 동의 후
       * 브라우저 위치 권한 요청
       */
      requestCurrentLocation();
    } catch (error) {
      console.error(error);

      setLocationError(
        error.message || "위치정보 이용 동의 처리에 실패했습니다."
      );
    } finally {
      setConsentSaving(false);
    }
  };

  /*
   * 기본 활동 지역 사용
   */
  const handleSavedLocationClick = () => {
    setLocationError("");

    if (!selectedRegion) {
      setRegionEditing(true);
      return;
    }

    setLocationMode("SAVED");
    setCurrentLocation(null);
  };

  /*
   * 위치 사용 안 함
   */
  const handleNoLocationClick = () => {
    setLocationMode("NONE");

    setCurrentLocation(null);
    setLocationError("");
  };

  /*
   * 감정 기록 저장
   */
  const handleSave = async () => {
    if (!emotion) {
      alert("감정을 선택해주세요.");
      return;
    }

    if (!currentActivity.trim()) {
      alert("지금 무엇을 하고 있는지 입력해주세요.");
      return;
    }

    if (!locationMode) {
      alert("추천 받을 위치를 선택해주세요.");
      return;
    }

    try {
      setSaving(true);

      const moodEntryId = await createMood({
        emotionCode: emotion,
        intensity,
        currentActivity: currentActivity.trim(),
        diaryContent: diary.trim()
      });

      /*
       * 감정 기록은 DB에 저장.
       *
       * 위치는 추천에서 사용하기 위해
       * 다음 페이지로 전달.
       */
      navigate("/recommendation", {
        state: {
          moodEntryId,
          locationMode,
          selectedRegion: locationMode === "SAVED" ? selectedRegion : null,
          currentLocation: locationMode === "CURRENT" ? currentLocation : null
        }
      });
    } catch (error) {
      console.error(error);

      alert(error.message || "감정 기록 저장 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="mood-page">
      <header className="page-header">
        <span>오늘의 기록</span>

        <h1>지금 마음은 어떤가요?</h1>

        <p>정답은 없어요. 지금 느끼는 그대로 남겨주세요.</p>
      </header>

      {/* =========================
          감정
      ========================= */}

      <section className="form-section">
        <h2>감정 선택</h2>

        <div className="emotion-grid">
          {EMOTION_OPTIONS.map((item) => (
            <button
              key={item.code}
              type="button"
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

      {/* =========================
          강도
      ========================= */}

      <section className="form-section">
        <h2>감정의 강도</h2>

        <div className="intensity-scale">
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((score) => (
            <button
              key={score}
              type="button"
              className={intensity === score ? "selected" : ""}
              onClick={() => setIntensity(score)}
            >
              {score}
            </button>
          ))}
        </div>
      </section>

      {/* =========================
          현재 활동
      ========================= */}

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

      {/* =========================
          추천 위치
      ========================= */}

      <section className="form-section">
        <div className="location-section-header">
          <div>
            <h2>추천 받을 위치</h2>

            <p>주변 장소를 추천할 때 사용할 위치를 선택해주세요.</p>
          </div>
        </div>

        <div className="location-option-list">
          {/* 기본 활동 지역 */}
          <div
            className={
              locationMode === "SAVED"
                ? "location-option selected"
                : "location-option"
            }
          >
            <button
              type="button"
              className="location-option-main"
              onClick={handleSavedLocationClick}
            >
              <span className="location-option-icon">🏠</span>

              <div className="location-option-copy">
                <strong>기본 활동 지역</strong>

                <span>
                  {selectedRegion
                    ? selectedRegion.regionName
                    : "등록된 기본 활동 지역이 없어요."}
                </span>
              </div>

              <span className="location-radio">
                {locationMode === "SAVED" ? "✓" : ""}
              </span>
            </button>

            <button
              type="button"
              className="location-edit-button"
              onClick={() => {
                setRegionEditing((prev) => !prev);

                setRegionError("");
                setRegionQuery("");
                setRegionResults([]);
              }}
            >
              {selectedRegion ? "수정" : "지역 설정"}
            </button>
          </div>

          {/* 지역 수정 */}
          {regionEditing && (
            <div className="mood-region-editor">
              <div className="mood-region-search">
                <input
                  type="text"
                  value={regionQuery}
                  placeholder="예: 봉천동"
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

              {regionError && (
                <p className="mood-location-error">{regionError}</p>
              )}

              {regionResults.length > 0 && (
                <div className="mood-region-results">
                  {regionResults.map((region) => (
                    <button
                      key={`${region.regionCode ?? "none"}-${region.latitude}-${region.longitude}`}
                      type="button"
                      className="mood-region-result"
                      onClick={() => handleRegionSelect(region)}
                      disabled={regionSaving}
                    >
                      <span>📍</span>

                      <div>
                        <strong>{region.regionName}</strong>

                        <p>기본 활동 지역으로 변경</p>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* 현재 위치 */}
          <div
            className={
              locationMode === "CURRENT"
                ? "location-option selected"
                : "location-option"
            }
          >
            <button
              type="button"
              className="location-option-main"
              onClick={handleCurrentLocationClick}
              disabled={locationLoading}
            >
              <span className="location-option-icon">📍</span>

              <div className="location-option-copy">
                <strong>현재 위치 사용</strong>

                <span>
                  {locationLoading
                    ? "현재 위치를 확인하고 있어요..."
                    : currentLocation
                      ? "현재 위치가 확인되었습니다."
                      : "지금 있는 곳을 기준으로 추천해요."}
                </span>
              </div>

              <span className="location-radio">
                {locationMode === "CURRENT" ? "✓" : ""}
              </span>
            </button>
          </div>

          {/* 위치 사용 안 함 */}
          <div
            className={
              locationMode === "NONE"
                ? "location-option selected"
                : "location-option"
            }
          >
            <button
              type="button"
              className="location-option-main"
              onClick={handleNoLocationClick}
            >
              <span className="location-option-icon">－</span>

              <div className="location-option-copy">
                <strong>위치 사용 안 함</strong>

                <span>주변 장소 없이 활동만 추천받아요.</span>
              </div>

              <span className="location-radio">
                {locationMode === "NONE" ? "✓" : ""}
              </span>
            </button>
          </div>
        </div>

        {locationError && (
          <p className="mood-location-error">{locationError}</p>
        )}
      </section>

      {/* =========================
          일기
      ========================= */}

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

      <button
        type="button"
        className="save-mood-button"
        onClick={handleSave}
        disabled={saving}
      >
        {saving ? "저장 중..." : "오늘의 감정 저장하기"}
      </button>

      {/* =========================
          위치 이용 약관 모달
      ========================= */}

      {consentModalOpen && (
        <div className="location-consent-overlay">
          <div
            className="location-consent-modal"
            role="dialog"
            aria-modal="true"
          >
            <div className="location-consent-icon">📍</div>

            <h2>{locationConsent?.title ?? "현재 위치 기반 추천"}</h2>

            <p className="location-consent-content">
              {locationConsent?.content ??
                "주변 장소 추천을 위해 현재 위치를 일시적으로 사용합니다."}
            </p>

            <div className="location-consent-notice">
              선택 동의이며, 동의하지 않아도 위치를 사용하지 않는 추천은 이용할
              수 있어요.
            </div>

            <div className="location-consent-buttons">
              <button
                type="button"
                className="location-consent-cancel"
                onClick={() => setConsentModalOpen(false)}
                disabled={consentSaving}
              >
                취소
              </button>

              <button
                type="button"
                className="location-consent-agree"
                onClick={handleConsentAgree}
                disabled={consentSaving}
              >
                {consentSaving ? "처리 중..." : "동의하고 현재 위치 사용"}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
