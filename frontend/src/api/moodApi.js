import { authFetch } from "./authApi";

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
    if (response.status === 400) {
      throw new Error("오늘의 감정 기록을 다시 확인해주세요.");
    }

    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("감정 기록 저장에 실패했습니다.");
  }

  return response.json();
}
