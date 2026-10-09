import { authFetch } from "./authApi";

const API_BASE_URL = "/api";

// 추천 생성 (POST)
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

    throw new Error("추천 행동을 생성하지 못했습니다.");
  }

  const data = await response.json();

  console.log("===== 추천 생성 응답 =====");
  console.log(data);

  return data;
}

// 저장된 추천 조회 (GET)
export async function getSavedRecommendations(moodEntryId) {
  if (!moodEntryId) {
    throw new Error("감정 기록 ID가 필요합니다.");
  }

  console.log("===== 저장된 추천 조회 요청 =====");
  console.log({ moodEntryId });

  const response = await authFetch(
    `${API_BASE_URL}/recommendations/mood-entries/${moodEntryId}`,
    {
      method: "GET",
    },
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    if (response.status === 403) {
      throw new Error("추천 결과에 접근할 권한이 없습니다.");
    }

    if (response.status === 404) {
      throw new Error("저장된 추천 결과를 찾을 수 없습니다.");
    }

    throw new Error("저장된 추천 행동을 불러오지 못했습니다.");
  }

  const data = await response.json();

  console.log("===== 저장된 추천 조회 응답 =====");
  console.log(data);

  return data;
}
// 행동 선택·재측정 API 공통 요청 처리
async function requestActionExecution(path, method = "GET", body) {
  const options = { method };

  if (body !== undefined) {
    options.headers = {
      "Content-Type": "application/json",
    };
    options.body = JSON.stringify(body);
  }

  const response = await authFetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    switch (response.status) {
      case 400:
        throw new Error("입력한 정보를 확인해 주세요.");
      case 401:
        throw new Error("로그인이 필요합니다.");
      case 403:
        throw new Error("접근 권한이 없습니다.");
      case 404:
        throw new Error("요청한 기록을 찾을 수 없습니다.");
      case 409:
        throw new Error(
          "이미 처리되었거나 현재 상태에서 진행할 수 없습니다. 화면을 새로고침해 주세요.",
        );
      default:
        throw new Error("요청을 처리하지 못했습니다. 다시 시도해 주세요.");
    }
  }

  return response.json();
}

// 행동 선택: 선택 즉시 실행 시작
export function selectRecommendation(sessionId, recommendationId) {
  return requestActionExecution(
    `/recommendations/sessions/${sessionId}/select`,
    "POST",
    { recommendationId },
  );
}

// 추천 3개 모두 건너뛰기
export function skipRecommendations(sessionId) {
  return requestActionExecution(
    `/recommendations/sessions/${sessionId}/skip`,
    "POST",
  );
}

// 실행 상태와 재측정 전후 점수 조회
export function getActionExecution(executionId) {
  return requestActionExecution(`/action-executions/${executionId}`);
}

// 슬라이더에서 선택한 기분 점수로 재측정 완료
export function recheckActionExecution(executionId, afterScore) {
  return requestActionExecution(
    `/action-executions/${executionId}/recheck`,
    "POST",
    { afterScore },
  );
}
