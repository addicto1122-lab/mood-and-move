import { authFetch } from "./authApi";

export async function getNearbyPlaces({ type, latitude, longitude, keyword }) {
  const params = new URLSearchParams({
    type,
    latitude: String(latitude),
    longitude: String(longitude),
  });

  if (keyword?.trim()) {
    params.set("keyword", keyword.trim());
  }

  const response = await authFetch(`/api/places/nearby?${params.toString()}`, {
    method: "GET",
  });

  if (!response.ok) {
    throw new Error("주변 장소를 불러오지 못했습니다.");
  }

  return response.json();
}
