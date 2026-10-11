import { useEffect, useState, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import {
  getSavedRecommendations,
  selectRecommendation,
  skipRecommendations,
} from "../api/recommendationApi";

import "./RecommendationPage.css";
import { getNearbyPlaces } from "../api/placeApi";
import NearbyPlaceMapModal from "../components/NearbyPlaceMapModal.jsx";

const environmentNames = {
  INDOOR: "실내",
  OUTDOOR: "실외",
  ANY: "어디서든",
};

export default function RecommendationPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  /*
   * URL에서 moodEntryId 조회
   *
   * 예시:
   * /recommendations?moodEntryId=42
   */
  const moodEntryId = searchParams.get("moodEntryId");

  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sessionId, setSessionId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState("");

  const submittingRef = useRef(false);

  const [placeRecommendationId, setPlaceRecommendationId] = useState(null);
  const [nearbyPlaces, setNearbyPlaces] = useState([]);
  const [placeLoading, setPlaceLoading] = useState(false);
  const [placeError, setPlaceError] = useState("");

  const [showPlaceMap, setShowPlaceMap] = useState(false);

  // const handleFindPlaces = (item) => {
  //   if (!item.locationRequired || item.placeCategory) {
  //     return;
  //   }

  //   if (!navigator.geolocation) {
  //     setPlaceError("현재 브라우저에서는 위치 기능을 사용할 수 없습니다.");
  //     return;
  //   }

  //   setPlaceLoading(true);
  //   setPlaceError("");
  //   setNearbyPlaces([]);
  //   setPlaceRecommendationId(item.recommendationId);

  //   navigator.geolocation.getCurrentPosition(
  //     async (position) => {
  //       try {
  //         const places = await getNearbyPlaces({
  //           type: item.placeCategory,
  //           latitude: position.coords.latitude,
  //           longitude: position.coords.longitude,
  //         });

  //         setNearbyPlaces(places);
  //       } catch (error) {
  //         console.error(error);

  //         setPlaceError("주변에서 적절한 장소를 찾지 못했습니다.");
  //       } finally {
  //         setPlaceLoading(false);
  //       }
  //     },
  //     () => {
  //       setPlaceError("주변 장소를 추천하려면 위치 사용을 허용해주세요.");

  //       setPlaceLoading(false);
  //     },
  //   );
  // };
  const handleFindPlaces = (item) => {
    console.log("===== 장소 버튼 클릭 =====");
    console.log("추천 item:", item);

    if (!item.locationRequired || !item.placeCategory) {
      console.log("장소 검색 조건 불충족", {
        locationRequired: item.locationRequired,
        placeCategory: item.placeCategory,
      });

      return;
    }

    console.log("장소 검색 조건 통과");
    console.log("placeCategory:", item.placeCategory);

    if (!navigator.geolocation) {
      setPlaceError("현재 브라우저에서는 위치 기능을 사용할 수 없습니다.");
      return;
    }

    setPlaceLoading(true);
    setPlaceError("");
    setNearbyPlaces([]);
    setShowPlaceMap(false);
    setPlaceRecommendationId(item.recommendationId);

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        console.log("===== 현재 위치 획득 성공 =====");
        console.log({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });

        try {
          console.log("===== 장소 API 호출 직전 =====");
          console.log({
            type: item.placeCategory,
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });

          const places = await getNearbyPlaces({
            type: item.placeCategory,
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });

          console.log("===== 장소 API 응답 =====");
          console.log(places);

          setNearbyPlaces(places);
        } catch (error) {
          console.error("===== 장소 API 호출 실패 =====");
          console.error(error);

          setPlaceError("주변에서 적절한 장소를 찾지 못했습니다.");
        } finally {
          setPlaceLoading(false);
        }
      },

      (error) => {
        console.error("===== 현재 위치 획득 실패 =====");
        console.error({
          code: error.code,
          message: error.message,
        });

        setPlaceError("현재 위치를 확인하지 못했습니다.");

        setPlaceLoading(false);
      },

      {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 60000,
      },
    );
  };

  /*
   * 저장된 추천 조회
   *
   * - 추천 생성 POST 호출하지 않음
   * - URL의 moodEntryId로 GET 요청
   * - 새로고침해도 동일한 추천 조회
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

        const data = await getSavedRecommendations(moodEntryId);

        if (cancelled) {
          return;
        }
        setSessionId(data.sessionId);
        setRecommendations(data.recommendations ?? []);
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

  // 선택 성공 후 메인으로 이동
  const handleStart = async (item) => {
    if (submittingRef.current || !sessionId) {
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setActionError("");

    try {
      await selectRecommendation(sessionId, item.recommendationId);

      navigate("/", { replace: true });
    } catch (err) {
      setActionError(err.message || "행동을 선택하지 못했습니다.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  // 건너뛰기 저장에 성공한 뒤 메인으로 이동
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

      navigate("/", { replace: true });
    } catch (err) {
      setActionError(err.message || "추천을 건너뛰지 못했습니다.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };
  /*
   * moodEntryId 없이 직접 진입한 경우
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
   * 저장된 추천 조회 중
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
   * 추천 조회 실패
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
   * 추천 결과가 비어 있는 경우
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
   * 추천 결과 화면
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

      <section className="recommendation-grid"></section>
      <section className="recommendation-grid">
        {recommendations.map((item) => (
          <article
            key={item.recommendationId}
            className={
              item.rankNo === 1
                ? "action-recommend-card best"
                : "action-recommend-card"
            }
          >
            <div className="card-top">
              <div className="card-icon" aria-hidden="true">
                {item.emoji?.trim() || item.categoryEmoji?.trim() || "✨"}
              </div>
              {item.rankNo === 1 && (
                <span className="best-badge">✨ 가장 잘 맞아요</span>
              )}
            </div>

            <h2>{item.actionName}</h2>

            {item.categoryName && (
              <p className="card-category">{item.categoryName}</p>
            )}
            <div className="card-meta">
              <span>◷ {item.durationMinutes}분</span>

              <span>
                ⌖ {environmentNames[item.environmentType] ?? "어디서든"}
              </span>
            </div>

            <p className="card-reason">{item.reason}</p>
            {item.locationRequired && item.placeCategory && (
              <button
                type="button"
                className="place-search-button"
                onClick={() => {
                  console.log("🔥 버튼 자체 클릭됨");
                  handleFindPlaces(item);
                }}
              >
                📍 내 주변 장소 보기
              </button>
            )}

            {placeRecommendationId === item.recommendationId && (
              <>
                {placeLoading && (
                  <p className="place-loading">주변 장소를 찾고 있어요...</p>
                )}

                {placeError && <p className="place-error">{placeError}</p>}

                {!placeLoading && !placeError && nearbyPlaces.length > 0 && (
                  <>
                    <div className="nearby-place-list">
                      {nearbyPlaces.map((place, index) => (
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

                    <button
                      type="button"
                      className="kakao-map-open-button"
                      onClick={() => setShowPlaceMap(true)}
                    >
                      🗺 카카오맵에서 보기
                    </button>
                  </>
                )}

                {!placeLoading && !placeError && nearbyPlaces.length === 0 && (
                  <p className="place-empty">
                    주변에서 적절한 장소를 찾지 못했어요.
                  </p>
                )}
              </>
            )}

            <button
              type="button"
              className="card-start-button"
              onClick={() => handleStart(item)}
              disabled={submitting || !sessionId}
            >
              {submitting ? "처리 중..." : "이 행동 선택하기"}
            </button>
          </article>
        ))}
      </section>

      <button
        type="button"
        className="recommendation-skip-button"
        onClick={handleSkip}
        disabled={submitting || !sessionId}
      >
        {submitting ? "처리 중..." : "이번 추천 건너뛰기"}
      </button>
      {showPlaceMap && nearbyPlaces.length > 0 && (
        <NearbyPlaceMapModal
          places={nearbyPlaces}
          onClose={() => setShowPlaceMap(false)}
        />
      )}
    </main>
  );
}
