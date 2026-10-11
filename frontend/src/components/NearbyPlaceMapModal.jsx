import { useEffect, useRef, useState } from "react";
import "../pages/RecommendationPage.css";

let kakaoMapSdkPromise = null;

function loadKakaoMapSdk() {
  if (window.kakao?.maps) {
    return new Promise((resolve) => {
      window.kakao.maps.load(resolve);
    });
  }

  if (kakaoMapSdkPromise) {
    return kakaoMapSdkPromise;
  }

  kakaoMapSdkPromise = new Promise((resolve, reject) => {
    const appKey = import.meta.env.VITE_KAKAO_MAP_JS_KEY;

    if (!appKey) {
      reject(new Error("VITE_KAKAO_MAP_JS_KEY가 설정되지 않았습니다."));
      return;
    }

    const script = document.createElement("script");

    script.src =
      `https://dapi.kakao.com/v2/maps/sdk.js` +
      `?appkey=${appKey}&autoload=false`;

    script.async = true;

    script.onload = () => {
      window.kakao.maps.load(resolve);
    };

    script.onerror = () => {
      reject(new Error("카카오맵 SDK를 불러오지 못했습니다."));
    };

    document.head.appendChild(script);
  });

  return kakaoMapSdkPromise;
}

export default function NearbyPlaceMapModal({ places, onClose }) {
  const mapRef = useRef(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function createMap() {
      try {
        setError("");

        await loadKakaoMapSdk();

        if (cancelled || !mapRef.current) {
          return;
        }

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

        const firstPlace = validPlaces[0];

        const map = new kakao.maps.Map(mapRef.current, {
          center: new kakao.maps.LatLng(
            Number(firstPlace.latitude),
            Number(firstPlace.longitude),
          ),
          level: 4,
        });

        const bounds = new kakao.maps.LatLngBounds();

        validPlaces.forEach((place, index) => {
          const position = new kakao.maps.LatLng(
            Number(place.latitude),
            Number(place.longitude),
          );

          // 기본 마커
          const marker = new kakao.maps.Marker({
            map,
            position,
            title: place.name,
          });

          /*
           * 마커 번호
           * 추천 목록의 1~5번과 지도 번호를 일치시킴
           */
          const numberOverlay = new kakao.maps.CustomOverlay({
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
           * 마커 클릭 시 장소 이름 표시
           */
          const infoContent = document.createElement("div");
          infoContent.className = "map-place-info";

          const name = document.createElement("strong");
          name.textContent = `${index + 1}. ${place.name}`;

          const distance = document.createElement("span");
          distance.textContent = `현재 위치에서 ${place.distance}m`;

          infoContent.appendChild(name);
          infoContent.appendChild(distance);

          const infoWindow = new kakao.maps.InfoWindow({
            content: infoContent,
            removable: true,
          });

          kakao.maps.event.addListener(marker, "click", () => {
            infoWindow.open(map, marker);
          });

          bounds.extend(position);
        });

        map.setBounds(bounds);
      } catch (err) {
        console.error(err);

        setError(err.message || "카카오맵을 표시하지 못했습니다.");
      }
    }

    createMap();

    return () => {
      cancelled = true;
    };
  }, [places]);

  return (
    <div className="place-map-overlay" onClick={onClose}>
      <section
        className="place-map-modal"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="place-map-header">
          <div>
            <span>NEARBY PLACES</span>
            <h2>추천 장소 한눈에 보기</h2>
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

        {error ? (
          <p className="place-map-error">{error}</p>
        ) : (
          <div ref={mapRef} className="place-map-canvas" />
        )}
        <div className="place-map-legend">
          {places.map((place, index) => (
            <div key={place.placeId} className="place-map-legend-item">
              <span>{index + 1}</span>

              <strong>{place.name}</strong>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
