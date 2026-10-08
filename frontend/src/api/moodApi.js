import { authFetch, getErrorMessage } from "./authApi";

const API_BASE_URL = "/api";

export async function createMood({
  emotionCode,
  intensity,
  currentActivity,
  diaryContent
}) {
  const response = await authFetch(`${API_BASE_URL}/moods`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      emotionCode,
      intensity,
      currentActivity,
      diaryContent
    })
  });

  if (!response.ok) {
    const fallbackMessage =
      response.status === 401
        ? "로그인이 필요합니다."
        : "감정 기록 저장에 실패했습니다.";

    throw new Error(await getErrorMessage(response, fallbackMessage));
  }

  return response.json();
}
