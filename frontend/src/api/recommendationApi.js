import { authFetch } from "./authApi";

const API_BASE_URL = "/api";

export async function generateRecommendations({
  moodEntryId,
  locationMode,
  currentLocation,
}) {
  const requestBody = {
    locationMode,

    latitude:
      locationMode === "CURRENT" ? (currentLocation?.latitude ?? null) : null,

    longitude:
      locationMode === "CURRENT" ? (currentLocation?.longitude ?? null) : null,
  };

  console.log("===== 추천 요청 =====");
  console.log({
    moodEntryId,
    ...requestBody,
  });

  const response = await authFetch(
    `${API_BASE_URL}/recommendations/${moodEntryId}/generate`,
    {
      method: "POST",

      headers: {
        "Content-Type": "application/json",
      },

      body: JSON.stringify(requestBody),
    },
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    if (response.status === 400) {
      throw new Error("추천 요청 정보를 다시 확인해주세요.");
    }

    throw new Error("추천 행동을 불러오지 못했습니다.");
  }

  const data = await response.json();

  console.log("===== 추천 응답 =====");
  console.log(data);

  return data;
}
