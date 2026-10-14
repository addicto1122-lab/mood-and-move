import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { formatDuration } from "../utils/formatDuration";
import {
  getSavedRecommendations,
  selectRecommendation,
  skipRecommendations,
  addDislike,
} from "../api/recommendationApi";

import { getNearbyPlaces } from "../api/placeApi";

import NearbyPlaceMapModal from "../components/NearbyPlaceMapModal.jsx";

import "./RecommendationPage.css";

const environmentNames = {
  INDOOR: "실내",
  OUTDOOR: "실외",
  ANY: "어디서든",
};

/*
 * =========================
 * 장소 캐시
 * =========================
 *
 * 같은 recommendationId의 장소 결과를
 * sessionStorage에 저장한다.
 *
 * F5 새로고침을 해도 유지되지만
 * 브라우저 탭을 닫으면 제거된다.
 */
const PLACE_CACHE_PREFIX = "moodmove:nearby-places:v2";

function getPlaceCacheKey(recommendationId, scope) {
  return PLACE_CACHE_PREFIX + JSON.stringify([scope, recommendationId]);
}

/*
 * 캐시 조회
 *
 * null
 * → 캐시 자체가 없음
 *
 * []
 * → 검색했지만 결과가 없었음
 *
 * [place, place...]
 * → 검색 결과 존재
 */
function loadCachedPlaces(recommendationId, scope) {
  try {
    const raw = sessionStorage.getItem(
      getPlaceCacheKey(recommendationId, scope),
    );

    if (raw === null) {
      return null;
    }

    const data = JSON.parse(raw);

    if (!Array.isArray(data)) {
      return null;
    }

    return data;
  } catch (error) {
    console.error("장소 캐시 조회 실패:", error);

    return null;
  }
}

/*
 * 캐시 저장
 */
function saveCachedPlaces(recommendationId, places, scope) {
  try {
    sessionStorage.setItem(
      getPlaceCacheKey(recommendationId, scope),
      JSON.stringify(places ?? []),
    );
  } catch (error) {
    console.error("장소 캐시 저장 실패:", error);
  }
}

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

  const [placeCacheScope, setPlaceCacheScope] = useState(null);

  const submittingRef = useRef(false);

  /*
   * =========================
   * 추천별 장소 검색 결과
   * =========================
   *
   * {
   *   101: [장소1, 장소2],
   *   102: [장소1]
   * }
   */
  const [placeResults, setPlaceResults] = useState({});

  /*
   * 추천별 장소 검색 오류
   */
  const [placeErrors, setPlaceErrors] = useState({});

  /*
   * 현재 장소 검색 중인
   * recommendationId
   */
  const [placeLoadingId, setPlaceLoadingId] = useState(null);

  /*
   * 장소 API 중복 호출 방지
   */
  const placeLoadingRef = useRef(false);

  /*
   * 현재 지도 모달에서
   * 보고 있는 recommendationId
   */
  const [mapRecommendationId, setMapRecommendationId] = useState(null);

  /*
   * =========================
   * 위치
   * =========================
   *
   * SAVED
   * CURRENT
   * NONE
   */
  const [locationMode, setLocationMode] = useState(null);

  /*
   * SAVED 위치 좌표
   */
  const [searchLocation, setSearchLocation] = useState(null);

  /*
   * =========================
   * 임시 비선호
   * =========================
   *
   * 체크 순간 DB 저장 X
   *
   * 행동 선택 / 추천 스킵 시
   * 체크된 행동만 DB 저장
   */
  const [pendingDislikeIds, setPendingDislikeIds] = useState([]);

  /*
   * =========================
   * 비선호 체크 / 해제
   * =========================
   */
  const handleDislikeChange = (actionId, checked) => {
    if (checked) {
      setPendingDislikeIds((prev) => {
        if (prev.includes(actionId)) {
          return prev;
        }

        return [...prev, actionId];
      });

      return;
    }

    setPendingDislikeIds((prev) => prev.filter((id) => id !== actionId));
  };

  /*
   * =========================
   * 임시 비선호 DB 저장
   * =========================
   */
  const savePendingDislikes = async () => {
    if (pendingDislikeIds.length === 0) {
      return;
    }

    await Promise.all(
      pendingDislikeIds.map((actionId) => addDislike(actionId)),
    );
  };

  /*
   * =========================
   * 주변 장소 검색
   * =========================
   */
  const handleFindPlaces = async (item) => {
    /*
     * 장소 추천이 필요 없는 행동
     */
    if (
      (locationMode !== "CURRENT" && locationMode !== "SAVED") ||
      !item.locationRequired ||
      !item.placeCategory
    ) {
      return;
    }

    const recommendationId = item.recommendationId;

    /*
     * =========================
     * 1. React State 캐시 확인
     * =========================
     */
    const cachedPlaces = placeResults[recommendationId];

    /*
     * 장소가 이미 있으면
     * API 호출 없이 바로 지도
     */
    if (cachedPlaces?.length > 0) {
      setMapRecommendationId(recommendationId);

      return;
    }

    /*
     * =========================
     * 2. sessionStorage 확인
     * =========================
     *
     * 새로고침 후 React state는 사라지지만
     * sessionStorage는 유지된다.
     */
    const storedPlaces = loadCachedPlaces(recommendationId, placeCacheScope);

    /*
     * null이 아니라는 것은
     * 이전에 한번 검색했다는 뜻.
     *
     * 빈 배열도 다시 API 호출하지 않는다.
     */
    if (storedPlaces !== null) {
      setPlaceResults((prev) => ({
        ...prev,

        [recommendationId]: storedPlaces,
      }));

      /*
       * 장소가 있다면 지도 열기
       */
      if (storedPlaces.length > 0) {
        setMapRecommendationId(recommendationId);
      }

      return;
    }

    /*
     * =========================
     * 3. 실제 장소 API 호출
     * =========================
     */

    if (placeLoadingRef.current) {
      return;
    }

    placeLoadingRef.current = true;

    setPlaceLoadingId(recommendationId);

    setPlaceErrors((prev) => ({
      ...prev,

      [recommendationId]: "",
    }));

    try {
      if (locationMode !== "CURRENT" && locationMode !== "SAVED") {
        throw new Error("위치 기반 장소 추천을 사용하지 않았습니다.");
      }

      if (
        !searchLocation ||
        !Number.isFinite(searchLocation.latitude) ||
        !Number.isFinite(searchLocation.longitude)
      ) {
        throw new Error(
          "이 추천에는 작성 당시 위치가 저장되어 있지 않습니다. 새 추천부터 장소 검색을 사용할 수 있어요.",
        );
      }

      /*
       * =========================
       * 실제 API 호출
       * =========================
       */
      const places = await getNearbyPlaces({
        recommendationId: item.recommendationId,
      });

      console.log("===== 장소 검색 결과 =====", places);

      const resultPlaces = places ?? [];

      /*
       * =========================
       * sessionStorage 저장
       * =========================
       *
       * 여기서 저장해놔야
       * 새로고침 후 재사용 가능
       */
      saveCachedPlaces(recommendationId, resultPlaces, placeCacheScope);

      /*
       * React state에도 저장
       */
      setPlaceResults((prev) => ({
        ...prev,

        [recommendationId]: resultPlaces,
      }));

      /*
       * 검색 성공하면
       * 첫 클릭부터 바로 지도 OPEN
       */
      if (resultPlaces.length > 0) {
        setMapRecommendationId(recommendationId);
      }
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

        setSearchLocation(null);

        setPendingDislikeIds([]);

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

        const loadedRecommendations = data.recommendations ?? [];

        const cacheScope = JSON.stringify({
          sessionId: data.sessionId,
          locationMode: data.locationMode,
          latitude: data.searchLatitude,
          longitude: data.searchLongitude,
          recommendations: loadedRecommendations.map((item) => ({
            id: item.recommendationId,
            actionName: item.actionName,
            locationRequired: item.locationRequired,
            placeCategory: item.placeCategory,
            placeKeyword: item.placeKeyword ?? null,
          })),
        });

        setPlaceCacheScope(cacheScope);

        setSessionId(data.sessionId);

        setLocationMode(data.locationMode);

        setSearchLocation(
          data.searchLatitude != null && data.searchLongitude != null
            ? {
                latitude: Number(data.searchLatitude),
                longitude: Number(data.searchLongitude),
              }
            : null,
        );

        setRecommendations(loadedRecommendations);

        /*
         * =========================
         * sessionStorage 장소 캐시 복구
         * =========================
         *
         * F5 직후에도 버튼이
         * "카카오맵에서 보기" 상태가 되도록
         * 기존 캐시를 React state로 복구한다.
         */
        const restoredPlaces = {};

        loadedRecommendations.forEach((recommendation) => {
          // 위치를 사용하지 않거나 장소 검색 대상이 아니면 캐시 복원 생략
          if (
            (data.locationMode !== "CURRENT" &&
              data.locationMode !== "SAVED") ||
            !recommendation.locationRequired ||
            !recommendation.placeCategory
          ) {
            return;
          }

          const cached = loadCachedPlaces(
            recommendation.recommendationId,
            cacheScope,
          );
          /*
           * null
           * → 검색한 적 없음
           *
           * [] 또는 [...]
           * → 검색한 적 있음
           */
          if (cached !== null) {
            restoredPlaces[recommendation.recommendationId] = cached;
          }
        });

        setPlaceResults(restoredPlaces);
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
   * 행동 선택
   * =========================
   */
  const handleStart = async (item) => {
    /*
     * 비선호 체크된 행동
     * 선택 불가
     */
    if (pendingDislikeIds.includes(item.actionId)) {
      return;
    }

    if (submittingRef.current || !sessionId) {
      return;
    }

    submittingRef.current = true;

    setSubmitting(true);

    setActionError("");

    try {
      /*
       * 체크한 비선호 저장
       */
      await savePendingDislikes();

      /*
       * 행동 선택
       */
      await selectRecommendation(sessionId, item.recommendationId);

      navigate("/", {
        replace: true,
      });
    } catch (err) {
      console.error("행동 선택 실패:", err);

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
      /*
       * 체크한 비선호 저장
       */
      await savePendingDislikes();

      /*
       * 추천 스킵
       */
      await skipRecommendations(sessionId);

      navigate("/", {
        replace: true,
      });
    } catch (err) {
      console.error("추천 건너뛰기 실패:", err);

      setActionError(err.message || "추천을 건너뛰지 못했습니다.");
    } finally {
      submittingRef.current = false;

      setSubmitting(false);
    }
  };

  /*
   * 현재 지도에 표시할 장소
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
   * Loading
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
   * Error
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
      {/* Header */}
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

      {/* =========================
          추천 카드
      ========================= */}
      <section className="recommendation-grid">
        {recommendations.map((item) => {
          const placesForItem = placeResults[item.recommendationId] ?? [];

          const placeErrorForItem = placeErrors[item.recommendationId] ?? "";

          const isPlaceLoading = placeLoadingId === item.recommendationId;

          const alreadyLoaded = placesForItem.length > 0;

          const hasSearched = Object.prototype.hasOwnProperty.call(
            placeResults,
            item.recommendationId,
          );

          const isPendingDislike = pendingDislikeIds.includes(item.actionId);

          return (
            <article
              key={item.recommendationId}
              className={
                item.rankNo === 1
                  ? "action-recommend-card best"
                  : "action-recommend-card"
              }
            >
              {/* 카드 상단 */}
              <div className="card-top">
                <div className="card-icon" aria-hidden="true">
                  {item.emoji?.trim() || item.categoryEmoji?.trim() || "✨"}
                </div>

                {item.rankNo === 1 && (
                  <span className="best-badge">✨ 가장 잘 맞아요</span>
                )}
              </div>

              {/* 행동 이름 */}
              <h2>{item.actionName}</h2>

              {item.categoryName && (
                <p className="card-category">{item.categoryName}</p>
              )}

              {/* 행동 정보 */}
              <div className="card-meta">
                <span>◷ {formatDuration(item)}</span>

                <span>
                  ⌖ {environmentNames[item.environmentType] ?? "어디서든"}
                </span>
              </div>

              {/* 추천 이유 */}
              <p className="card-reason">{item.reason}</p>

              {/* =========================
                    장소 버튼
                ========================= */}
              {(locationMode === "CURRENT" || locationMode === "SAVED") &&
                item.locationRequired &&
                item.placeCategory && (
                  <button
                    type="button"
                    className="place-search-button"
                    onClick={() => {
                      /*
                       * React state에
                       * 이미 장소가 있으면
                       * 바로 지도
                       */
                      if (alreadyLoaded) {
                        setMapRecommendationId(item.recommendationId);

                        return;
                      }

                      /*
                       * state에 없으면
                       * handleFindPlaces 안에서
                       *
                       * sessionStorage →
                       * API 순서로 확인
                       */
                      handleFindPlaces(item);
                    }}
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

              {/* 장소 검색 중 */}
              {isPlaceLoading && (
                <p className="place-loading">주변 장소를 찾고 있어요...</p>
              )}

              {/* 장소 검색 오류 */}
              {placeErrorForItem && (
                <p className="place-error">{placeErrorForItem}</p>
              )}

              {/* 검색했지만 결과 없음 */}
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
                disabled={submitting || !sessionId || isPendingDislike}
              >
                {isPendingDislike
                  ? "비선호로 선택됨"
                  : submitting
                    ? "처리 중..."
                    : "이 행동 선택하기"}
              </button>

              {/* =========================
                    비선호
                ========================= */}
              <label className="dislike-checkbox">
                <input
                  type="checkbox"
                  checked={isPendingDislike}
                  disabled={submitting}
                  onChange={(e) =>
                    handleDislikeChange(item.actionId, e.target.checked)
                  }
                />

                <span>이 행동은 추천받고 싶지 않아요</span>
              </label>
            </article>
          );
        })}
      </section>

      {/* 추천 건너뛰기 */}
      <button
        type="button"
        className="recommendation-skip-button"
        onClick={handleSkip}
        disabled={submitting || !sessionId}
      >
        {submitting ? "처리 중..." : "이번 추천 건너뛰기"}
      </button>

      {/* Kakao Map Modal */}
      {mapRecommendationId != null && mapPlaces.length > 0 && (
        <NearbyPlaceMapModal
          places={mapPlaces}
          onClose={() => setMapRecommendationId(null)}
        />
      )}
    </main>
  );
}
