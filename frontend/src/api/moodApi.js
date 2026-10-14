import { authFetch, getErrorMessage } from "./authApi";

const API_BASE_URL = "/api";

export async function createMoodWithRecommendations({ mood, recommendation }) {
  const response = await authFetch(
    `${API_BASE_URL}/moods/with-recommendations`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        mood,
        recommendation,
      }),
    },
  );

  if (!response.ok) {
    throw new Error(
      await getErrorMessage(response, "일기 및 추천 처리에 실패했습니다."),
    );
  }

  return response.json();
}
