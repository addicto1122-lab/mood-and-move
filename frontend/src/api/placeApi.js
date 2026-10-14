import { authFetch } from "./authApi";

export async function getNearbyPlaces({ recommendationId }) {
  const params = new URLSearchParams({
    recommendationId: String(recommendationId),
  });

  const response = await authFetch(`/api/places/nearby?${params.toString()}`, {
    method: "GET",
  });

  if (!response.ok) {
    throw new Error("주변 장소를 불러오지 못했습니다.");
  }

  return response.json();
}
