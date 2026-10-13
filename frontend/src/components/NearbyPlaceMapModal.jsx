import { useEffect, useRef, useState } from "react";

import "../pages/RecommendationPage.css";

/*
 * =========================
 * Kakao Map SDK Promise
 * =========================
 */
let kakaoMapSdkPromise = null;

/*
 * =========================
 * Kakao Map SDK 로드
 * =========================
 */
function loadKakaoMapSdk() {
  /*
   * 이미 SDK가 로드된 경우
   */
  if (window.kakao?.maps) {
    return new Promise((resolve) => {
      window.kakao.maps.load(resolve);
    });
  }

  /*
   * 이미 로딩 중이면
   * 기존 Promise 재사용
   */
  if (kakaoMapSdkPromise) {
    return kakaoMapSdkPromise;
  }

  kakaoMapSdkPromise = new Promise((resolve, reject) => {
    const appKey = import.meta.env.VITE_KAKAO_MAP_JS_KEY;

    if (!appKey) {
      reject(new Error("VITE_KAKAO_MAP_JS_KEY가 설정되지 않았습니다."));

      return;
    }

    /*
     * 이미 script 태그가 있는지 확인
     */
    const existingScript = document.querySelector(
      'script[data-kakao-map-sdk="true"]',
    );

    if (existingScript) {
      existingScript.addEventListener(
        "load",
        () => {
          window.kakao.maps.load(resolve);
        },
        {
          once: true,
        },
      );

      existingScript.addEventListener(
        "error",
        () => {
          reject(new Error("카카오맵 SDK를 불러오지 못했습니다."));
        },
        {
          once: true,
        },
      );

      return;
    }

    /*
     * SDK script 생성
     */
    const script = document.createElement("script");

    script.src =
      "https://dapi.kakao.com/v2/maps/sdk.js" +
      `?appkey=${appKey}` +
      "&autoload=false";

    script.async = true;

    script.dataset.kakaoMapSdk = "true";

    script.onload = () => {
      if (!window.kakao?.maps) {
        reject(new Error("카카오맵 SDK 초기화에 실패했습니다."));

        return;
      }

      window.kakao.maps.load(resolve);
    };

    script.onerror = () => {
      /*
       * 실패 Promise 초기화
       */
      kakaoMapSdkPromise = null;

      reject(new Error("카카오맵 SDK를 불러오지 못했습니다."));
    };

    document.head.appendChild(script);
  });

  return kakaoMapSdkPromise;
}

/*
 * =========================
 * 추천 장소 지도 모달
 * =========================
 */
export default function NearbyPlaceMapModal({ places, onClose }) {
  const mapRef = useRef(null);

  const [error, setError] = useState("");

  /*
   * =========================
   * 지도 생성
   * =========================
   */
  useEffect(() => {
    let cancelled = false;

    async function createMap() {
      try {
        setError("");

        /*
         * Kakao SDK 로드
         */
        await loadKakaoMapSdk();

        if (cancelled || !mapRef.current) {
          return;
        }

        /*
         * 정상 좌표를 가진 장소만 사용
         */
        const validPlaces = places.filter(
          (place) =>
            Number.isFinite(Number(place.latitude)) &&
            Number.isFinite(Number(place.longitude)),
        );

        if (validPlaces.length === 0) {
          setError("지도에 표시할 장소가 없습니다.");

          return;
        }

        const kakao = window.kakao;

        /*
         * 첫 장소 위치
         */
        const firstPlace = validPlaces[0];

        const firstPosition = new kakao.maps.LatLng(
          Number(firstPlace.latitude),

          Number(firstPlace.longitude),
        );

        /*
         * 지도 생성
         */
        const map = new kakao.maps.Map(mapRef.current, {
          center: firstPosition,

          level: 4,
        });

        /*
         * 전체 장소 범위
         */
        const bounds = new kakao.maps.LatLngBounds();

        /*
         * =========================
         * InfoWindow 하나만 생성
         * =========================
         *
         * 여러 장소마다
         * InfoWindow를 만드는 것이 아니라
         * 하나를 계속 재사용한다.
         */
        const infoWindow = new kakao.maps.InfoWindow({
          removable: true,
        });

        /*
         * 현재 열려 있는 마커
         *
         * 같은 마커를 다시 클릭하면
         * 정보창을 닫기 위해 사용
         */
        let openedMarker = null;

        /*
         * =========================
         * 추천 장소 마커 생성
         * =========================
         */
        validPlaces.forEach((place, index) => {
          const position = new kakao.maps.LatLng(
            Number(place.latitude),

            Number(place.longitude),
          );

          /*
           * 기본 Kakao 마커
           */
          const marker = new kakao.maps.Marker({
            map,
            position,
            title: place.name,
          });

          /*
           * 추천 순위 번호
           */
          new kakao.maps.CustomOverlay({
            map,
            position,

            content: `
                <div class="map-marker-number">
                  ${index + 1}
                </div>
              `,

            yAnchor: 2.8,
          });

          /*
           * =========================
           * InfoWindow 내용
           * =========================
           */

          const infoContent = document.createElement("div");

          infoContent.className = "map-place-info";

          /*
           * 장소 이름
           */
          const name = document.createElement("strong");

          name.textContent = `${index + 1}. ${place.name}`;

          /*
           * 주소
           */
          const address = document.createElement("span");

          address.textContent = place.roadAddress || place.address || "";

          /*
           * 거리
           */
          const distance = document.createElement("span");

          if (place.distance != null) {
            distance.textContent = `기준 위치에서 ${place.distance}m`;
          }

          infoContent.appendChild(name);

          if (address.textContent) {
            infoContent.appendChild(address);
          }

          if (distance.textContent) {
            infoContent.appendChild(distance);
          }

          /*
           * =========================
           * 마커 클릭
           * =========================
           */
          kakao.maps.event.addListener(marker, "click", () => {
            /*
             * 같은 마커를
             * 다시 클릭했다면
             *
             * → 정보창 닫기
             */
            if (openedMarker === marker) {
              infoWindow.close();

              openedMarker = null;

              return;
            }

            /*
             * 다른 마커 클릭
             *
             * 기존 정보창 닫기
             */
            infoWindow.close();

            /*
             * 현재 장소 정보로
             * 내용 변경
             */
            infoWindow.setContent(infoContent);

            /*
             * 클릭한 마커에
             * 정보창 표시
             */
            infoWindow.open(map, marker);

            openedMarker = marker;
          });

          /*
           * 지도 범위 추가
           */
          bounds.extend(position);
        });

        /*
         * =========================
         * 지도 빈 공간 클릭
         * =========================
         *
         * 열려 있는 InfoWindow 닫기
         */
        kakao.maps.event.addListener(map, "click", () => {
          infoWindow.close();

          openedMarker = null;
        });

        /*
         * 추천 장소가 모두
         * 지도에 들어오도록 조절
         */
        if (validPlaces.length === 1) {
          map.setCenter(firstPosition);

          map.setLevel(4);
        } else {
          map.setBounds(bounds);
        }

        /*
         * 모달 렌더링 직후
         * 지도 크기 재계산
         */
        setTimeout(() => {
          if (cancelled || !mapRef.current) {
            return;
          }

          map.relayout();

          if (validPlaces.length > 1) {
            map.setBounds(bounds);
          }
        }, 0);
      } catch (err) {
        console.error("카카오맵 생성 실패:", err);

        if (cancelled) {
          return;
        }

        setError(err.message || "카카오맵을 표시하지 못했습니다.");
      }
    }

    createMap();

    return () => {
      cancelled = true;
    };
  }, [places]);

  /*
   * =========================
   * ESC → 모달 닫기
   * =========================
   */
  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  return (
    <div className="place-map-overlay" onClick={onClose}>
      <section
        className="place-map-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="place-map-title"
        onClick={(event) => event.stopPropagation()}
      >
        {/* =========================
            Header
        ========================= */}

        <div className="place-map-header">
          <div>
            <span>NEARBY PLACES</span>

            <h2 id="place-map-title">추천 장소 한눈에 보기</h2>
          </div>

          <button
            type="button"
            className="place-map-close"
            onClick={onClose}
            aria-label="지도 닫기"
          >
            ×
          </button>
        </div>

        {/* =========================
            Map
        ========================= */}

        {error ? (
          <p className="place-map-error">{error}</p>
        ) : (
          <div ref={mapRef} className="place-map-canvas" />
        )}

        {/* =========================
            장소 목록
        ========================= */}

        {!error && (
          <div className="place-map-legend">
            {places.map((place, index) => (
              <div key={place.placeId} className="place-map-legend-item">
                <span>{index + 1}</span>

                <div className="place-map-legend-info">
                  <strong>{place.name}</strong>

                  {place.distance != null && <small>{place.distance}m</small>}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
