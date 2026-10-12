import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { getPreferences } from "../api/authApi";

import {
  getSavedRecommendations,
  selectRecommendation,
  skipRecommendations,
} from "../api/recommendationApi";

import { getNearbyPlaces } from "../api/placeApi";

import NearbyPlaceMapModal from "../components/NearbyPlaceMapModal.jsx";

import "./RecommendationPage.css";

const environmentNames = {
  INDOOR: "실내",
  OUTDOOR: "실외",
  ANY: "어디서든",
};

export default function RecommendationPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  /*
   * URL
   *
   * /recommendation?moodEntryId=23
   */
  const moodEntryId = searchParams.get("moodEntryId");

  /*
   * =========================
   * 추천 상태
   * =========================
   */
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [sessionId, setSessionId] = useState(null);

  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState("");

  const submittingRef = useRef(false);

  /*
   * =========================
   * 추천별 장소 검색 결과
   * =========================
   *
   * 예:
   *
   * {
   *   101: [장소1, 장소2, 장소3],
   *   102: [장소1, 장소2]
   * }
   *
   * recommendationId를 key로 사용한다.
   */
  const [placeResults, setPlaceResults] = useState({});

  /*
   * =========================
   * 추천별 장소 검색 오류
   * =========================
   *
   * {
   *   101: "",
   *   102: "장소 검색 실패"
   * }
   */
  const [placeErrors, setPlaceErrors] = useState({});

  /*
   * 현재 장소를 검색하고 있는
   * recommendationId
   */
  const [placeLoadingId, setPlaceLoadingId] = useState(null);

  /*
   * 장소 API 중복 호출 방지
   */
  const placeLoadingRef = useRef(false);

  /*
   * 현재 지도 모달에서 보고 있는
   * recommendationId
   */
  const [mapRecommendationId, setMapRecommendationId] = useState(null);

  /*
   * =========================
   * 위치 방식
   * =========================
   *
   * SAVED
   * CURRENT
   * NONE
   */
  const [locationMode, setLocationMode] = useState(null);

  /*
   * 기본 활동 지역 좌표
   *
   * {
   *   latitude: ...,
   *   longitude: ...
   * }
   */
  const [savedLocation, setSavedLocation] = useState(null);

  /*
   * =========================
   * 장소 검색
   * =========================
   */
  const handleFindPlaces = async (item) => {
    /*
     * 장소가 필요하지 않은 추천이면 종료
     */
    if (!item.locationRequired || !item.placeCategory) {
      return;
    }

    const recommendationId = item.recommendationId;

    /*
     * 이미 해당 추천 카드에서
     * 장소 검색을 완료했다면
     *
     * 장소 API를 다시 호출하지 않고
     * 바로 지도 모달을 연다.
     */
    const cachedPlaces = placeResults[recommendationId];

    if (cachedPlaces?.length > 0) {
      setMapRecommendationId(recommendationId);
      return;
    }

    /*
     * 다른 장소 검색이 진행 중이면
     * 중복 API 호출 방지
     */
    if (placeLoadingRef.current) {
      return;
    }

    placeLoadingRef.current = true;

    setPlaceLoadingId(recommendationId);

    /*
     * 현재 추천 카드의 기존 에러 초기화
     */
    setPlaceErrors((prev) => ({
      ...prev,
      [recommendationId]: "",
    }));

    try {
      let latitude;
      let longitude;

      /*
       * =========================
       * SAVED
       * 기본 활동 지역
       * =========================
       */
      if (locationMode === "SAVED") {
        if (
          savedLocation?.latitude == null ||
          savedLocation?.longitude == null
        ) {
          throw new Error("저장된 기본 활동 지역을 확인할 수 없습니다.");
        }

        latitude = savedLocation.latitude;
        longitude = savedLocation.longitude;
      } else if (locationMode === "CURRENT") {
        /*
         * =========================
         * CURRENT
         * 현재 위치
         * =========================
         */
        if (!navigator.geolocation) {
          throw new Error(
            "현재 브라우저에서는 위치 기능을 사용할 수 없습니다.",
          );
        }

        const position = await new Promise((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, {
            enableHighAccuracy: false,
            timeout: 10000,
            maximumAge: 60000,
          });
        });

        latitude = position.coords.latitude;
        longitude = position.coords.longitude;
      } else {
        /*
         * =========================
         * NONE
         * 위치 사용 안 함
         * =========================
         */
        throw new Error("위치 기반 장소 추천을 사용하지 않았습니다.");
      }

      console.log("===== 장소 검색 기준 =====");

      console.log({
        recommendationId,
        locationMode,
        latitude,
        longitude,
        placeCategory: item.placeCategory,
      });

      /*
       * 장소 API 호출
       */
      const places = await getNearbyPlaces({
        type: item.placeCategory,
        latitude,
        longitude,
      });

      console.log("===== 장소 검색 결과 =====");

      console.log(places);

      /*
       * recommendationId별로
       * 검색 결과를 각각 저장
       *
       * 기존 다른 카드 결과는 유지된다.
       */
      setPlaceResults((prev) => ({
        ...prev,
        [recommendationId]: places ?? [],
      }));
    } catch (error) {
      console.error("장소 검색 실패:", error);

      let message = error.message || "주변에서 적절한 장소를 찾지 못했습니다.";

      /*
       * 브라우저 위치 권한 거부
       */
      if (locationMode === "CURRENT" && error?.code === 1) {
        message = "주변 장소를 추천하려면 위치 사용을 허용해주세요.";
      }

      setPlaceErrors((prev) => ({
        ...prev,
        [recommendationId]: message,
      }));
    } finally {
      placeLoadingRef.current = false;
      setPlaceLoadingId(null);
    }
  };

  /*
   * =========================
   * 저장된 추천 조회
   * =========================
   *
   * 추천 생성 API를 다시 호출하지 않는다.
   *
   * moodEntryId로 이미 생성된
   * 추천 결과를 조회한다.
   */
  useEffect(() => {
    if (!moodEntryId) {
      return;
    }

    let cancelled = false;

    async function loadRecommendations() {
      try {
        setLoading(true);

        setError("");
        setActionError("");

        setSessionId(null);
        setRecommendations([]);

        setLocationMode(null);
        setSavedLocation(null);

        /*
         * 다른 moodEntry로 이동했을 경우
         * 장소 검색 캐시도 초기화
         */
        setPlaceResults({});
        setPlaceErrors({});
        setPlaceLoadingId(null);
        setMapRecommendationId(null);

        placeLoadingRef.current = false;

        /*
         * 저장된 추천 조회
         */
        const data = await getSavedRecommendations(moodEntryId);

        if (cancelled) {
          return;
        }

        console.log("===== 저장된 추천 조회 =====", data);

        setSessionId(data.sessionId);

        /*
         * SAVED / CURRENT / NONE
         */
        setLocationMode(data.locationMode);

        setRecommendations(data.recommendations ?? []);

        /*
         * =========================
         * SAVED
         * =========================
         *
         * 기본 활동 지역을 선택해서
         * 추천을 생성했다면
         * UserPreference의 좌표 조회
         */
        if (data.locationMode === "SAVED") {
          const preference = await getPreferences();

          if (cancelled) {
            return;
          }

          console.log("===== 기본 활동 지역 =====", {
            region: preference.defaultRegionName,

            latitude: preference.defaultRegionLatitude,

            longitude: preference.defaultRegionLongitude,
          });

          setSavedLocation({
            latitude: preference.defaultRegionLatitude,

            longitude: preference.defaultRegionLongitude,
          });
        }
      } catch (err) {
        console.error("추천 조회 실패:", err);

        if (cancelled) {
          return;
        }

        setError(err.message || "추천 행동을 불러오지 못했습니다.");
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadRecommendations();

    return () => {
      cancelled = true;
    };
  }, [moodEntryId]);

  /*
   * =========================
   * 추천 행동 선택
   * =========================
   */
  const handleStart = async (item) => {
    if (submittingRef.current || !sessionId) {
      return;
    }

    submittingRef.current = true;

    setSubmitting(true);
    setActionError("");

    try {
      await selectRecommendation(sessionId, item.recommendationId);

      navigate("/", {
        replace: true,
      });
    } catch (err) {
      setActionError(err.message || "행동을 선택하지 못했습니다.");
    } finally {
      submittingRef.current = false;

      setSubmitting(false);
    }
  };

  /*
   * =========================
   * 추천 건너뛰기
   * =========================
   */
  const handleSkip = async () => {
    if (submittingRef.current || !sessionId) {
      return;
    }

    if (!window.confirm("이번 추천을 모두 건너뛸까요?")) {
      return;
    }

    submittingRef.current = true;

    setSubmitting(true);
    setActionError("");

    try {
      await skipRecommendations(sessionId);

      navigate("/", {
        replace: true,
      });
    } catch (err) {
      setActionError(err.message || "추천을 건너뛰지 못했습니다.");
    } finally {
      submittingRef.current = false;

      setSubmitting(false);
    }
  };

  /*
   * =========================
   * 현재 지도에 표시할 장소
   * =========================
   */
  const mapPlaces =
    mapRecommendationId != null
      ? (placeResults[mapRecommendationId] ?? [])
      : [];

  /*
   * =========================
   * moodEntryId 없음
   * =========================
   */
  if (!moodEntryId) {
    return (
      <main className="recommendation-page">
        <header className="recommendation-header">
          <div>
            <span className="recommendation-eyebrow">맞춤 행동 추천</span>

            <h1>추천할 감정 기록이 없어요.</h1>

            <p>오늘의 감정을 먼저 기록해주세요.</p>

            <button type="button" onClick={() => navigate("/mood")}>
              감정 기록하기
            </button>
          </div>
        </header>
      </main>
    );
  }

  /*
   * =========================
   * 로딩
   * =========================
   */
  if (loading) {
    return (
      <main className="recommendation-page">
        <header className="recommendation-header">
          <div>
            <span className="recommendation-eyebrow">맞춤 행동 추천</span>

            <h1>추천 행동을 불러오고 있어요...</h1>

            <p>
              저장된 추천 결과를 확인하고 있어요.
              <br />
              잠시만 기다려주세요.
            </p>
          </div>
        </header>
      </main>
    );
  }

  /*
   * =========================
   * 추천 조회 실패
   * =========================
   */
  if (error) {
    return (
      <main className="recommendation-page">
        <header className="recommendation-header">
          <div>
            <span className="recommendation-eyebrow">추천 오류</span>

            <h1>추천을 불러오지 못했어요.</h1>

            <p>{error}</p>

            <button type="button" onClick={() => navigate("/mood")}>
              감정 기록으로 이동
            </button>
          </div>
        </header>
      </main>
    );
  }

  /*
   * =========================
   * 추천 없음
   * =========================
   */
  if (recommendations.length === 0) {
    return (
      <main className="recommendation-page">
        <header className="recommendation-header">
          <div>
            <span className="recommendation-eyebrow">맞춤 행동 추천</span>

            <h1>표시할 추천 행동이 없어요.</h1>

            <p>저장된 추천 결과를 확인할 수 없습니다.</p>

            <button type="button" onClick={() => navigate("/mood")}>
              감정 기록으로 이동
            </button>
          </div>
        </header>
      </main>
    );
  }

  /*
   * =========================
   * 추천 화면
   * =========================
   */
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
            지금의 감정과 상황을 고려해
            <br />
            부담 없이 할 수 있는 행동을 골랐어요.
          </p>
        </div>
      </header>

      {actionError && <p role="alert">{actionError}</p>}

      <section className="recommendation-grid">
        {recommendations.map((item) => {
          /*
           * 이 추천 카드에 저장된
           * 장소 검색 결과
           */
          const placesForItem = placeResults[item.recommendationId] ?? [];

          /*
           * 이 추천 카드의 장소 검색 오류
           */
          const placeErrorForItem = placeErrors[item.recommendationId] ?? "";

          /*
           * 현재 이 추천 카드가
           * 장소 검색 중인지
           */
          const isPlaceLoading = placeLoadingId === item.recommendationId;

          /*
           * 이 추천 카드에서
           * 장소 검색을 성공한 적이 있는지
           */
          const alreadyLoaded = placesForItem.length > 0;

          /*
           * 장소 검색 자체를 한 적이 있는지
           *
           * 결과가 0건일 때와
           * 아직 검색하지 않았을 때를 구분한다.
           */
          const hasSearched = Object.prototype.hasOwnProperty.call(
            placeResults,
            item.recommendationId,
          );

          return (
            <article
              key={item.recommendationId}
              className={
                item.rankNo === 1
                  ? "action-recommend-card best"
                  : "action-recommend-card"
              }
            >
              {/* =========================
                  카드 상단
              ========================= */}

              <div className="card-top">
                <div className="card-icon" aria-hidden="true">
                  {item.emoji?.trim() || item.categoryEmoji?.trim() || "✨"}
                </div>

                {item.rankNo === 1 && (
                  <span className="best-badge">✨ 가장 잘 맞아요</span>
                )}
              </div>

              {/* =========================
                  행동 이름
              ========================= */}

              <h2>{item.actionName}</h2>

              {item.categoryName && (
                <p className="card-category">{item.categoryName}</p>
              )}

              {/* =========================
                  행동 정보
              ========================= */}

              <div className="card-meta">
                <span>◷ {item.durationMinutes}분</span>

                <span>
                  ⌖ {environmentNames[item.environmentType] ?? "어디서든"}
                </span>
              </div>

              <p className="card-reason">{item.reason}</p>

              {/* =========================
                  장소 버튼
              ========================= */}

              {item.locationRequired && item.placeCategory && (
                <button
                  type="button"
                  className="place-search-button"
                  onClick={() => {
                    /*
                     * 이미 장소를 조회했다면
                     *
                     * 장소 API 재호출 X
                     * 지도 모달만 OPEN
                     */
                    if (alreadyLoaded) {
                      setMapRecommendationId(item.recommendationId);

                      return;
                    }

                    /*
                     * 아직 검색하지 않았다면
                     * 장소 API 호출
                     */
                    handleFindPlaces(item);
                  }}
                  /*
                   * 하나의 장소 검색이 진행 중이면
                   * 모든 장소 버튼 잠시 비활성화
                   *
                   * 동시에 여러 API 호출되는 것 방지
                   */
                  disabled={placeLoadingId !== null}
                >
                  {isPlaceLoading
                    ? "📍 주변 장소 찾는 중..."
                    : alreadyLoaded
                      ? "🗺️ 카카오맵에서 보기"
                      : locationMode === "SAVED"
                        ? "🏠 기본 활동 지역 장소 보기"
                        : "📍 내 주변 장소 보기"}
                </button>
              )}

              {/* =========================
                  장소 검색 로딩
              ========================= */}

              {isPlaceLoading && (
                <p className="place-loading">주변 장소를 찾고 있어요...</p>
              )}

              {/* =========================
                  장소 검색 오류
              ========================= */}

              {placeErrorForItem && (
                <p className="place-error">{placeErrorForItem}</p>
              )}

              {/* =========================
                  장소 목록
              ========================= */}

              {!isPlaceLoading &&
                !placeErrorForItem &&
                placesForItem.length > 0 && (
                  <div className="nearby-place-list">
                    {placesForItem.map((place, index) => (
                      <div key={place.placeId} className="nearby-place-card">
                        <div className="nearby-place-rank">{index + 1}</div>

                        <div className="nearby-place-info">
                          <strong>{place.name}</strong>

                          <span>{place.roadAddress || place.address}</span>
                        </div>

                        <span className="nearby-place-distance">
                          {place.distance}m
                        </span>
                      </div>
                    ))}
                  </div>
                )}

              {/* =========================
                  장소 결과 없음
              ========================= */}

              {!isPlaceLoading &&
                !placeErrorForItem &&
                hasSearched &&
                placesForItem.length === 0 && (
                  <p className="place-empty">
                    주변에서 적절한 장소를 찾지 못했어요.
                  </p>
                )}

              {/* =========================
                  행동 선택
              ========================= */}

              <button
                type="button"
                className="card-start-button"
                onClick={() => handleStart(item)}
                disabled={submitting || !sessionId}
              >
                {submitting ? "처리 중..." : "이 행동 선택하기"}
              </button>
            </article>
          );
        })}
      </section>

      {/* =========================
          추천 건너뛰기
      ========================= */}

      <button
        type="button"
        className="recommendation-skip-button"
        onClick={handleSkip}
        disabled={submitting || !sessionId}
      >
        {submitting ? "처리 중..." : "이번 추천 건너뛰기"}
      </button>

      {/* =========================
          Kakao Map Modal
      ========================= */}

      {mapRecommendationId != null && mapPlaces.length > 0 && (
        <NearbyPlaceMapModal
          places={mapPlaces}
          onClose={() => setMapRecommendationId(null)}
        />
      )}
    </main>
  );
}
