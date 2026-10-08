import { authFetch } from "./authApi";

const API_BASE_URL = "/api";

// 홈 화면 데이터 조회
export async function getHome() {
  const response = await authFetch(`${API_BASE_URL}/home`, {
    method: "GET"
  });

  if (!response.ok) {
    throw new Error("홈 화면 정보를 불러오지 못했습니다.");
  }

  return response.json();
}
