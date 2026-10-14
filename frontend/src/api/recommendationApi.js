import { authFetch } from "./authApi";

const API_BASE_URL = "/api";

/* =========================================================
   추천 생성
========================================================= */

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

/* =========================================================
   저장된 추천 조회
========================================================= */

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

/* =========================================================
   행동 선택 / 스킵 / 재측정 공통 요청
========================================================= */

async function requestActionExecution(path, method = "GET", body) {
  const options = {
    method,
  };

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

  /*
   * 204 No Content
   *
   * 추천 스킵처럼
   * 성공했지만 응답 body가 없는 경우
   * response.json()을 호출하면 오류가 발생한다.
   */
  if (response.status === 204) {
    return null;
  }

  return response.json();
}

/* =========================================================
   행동 선택
========================================================= */

// 선택 즉시 ActionExecution STARTED 생성
export function selectRecommendation(sessionId, recommendationId) {
  return requestActionExecution(
    `/recommendations/sessions/${sessionId}/select`,
    "POST",
    {
      recommendationId,
    },
  );
}

/* =========================================================
   추천 건너뛰기
========================================================= */

export function skipRecommendations(sessionId) {
  return requestActionExecution(
    `/recommendations/sessions/${sessionId}/skip`,
    "POST",
  );
}

/* =========================================================
   실행 상태 조회
========================================================= */

export function getActionExecution(executionId) {
  return requestActionExecution(`/action-executions/${executionId}`);
}

/* =========================================================
   재측정
========================================================= */

export function recheckActionExecution(executionId, afterScore) {
  return requestActionExecution(
    `/action-executions/${executionId}/recheck`,
    "POST",
    {
      afterScore,
    },
  );
}

/* =========================================================
   비선호 행동 조회
========================================================= */

export const getDislikeActions = async () => {
  const response = await authFetch(`${API_BASE_URL}/users/me/dislikes`, {
    method: "GET",
  });

  if (!response.ok) {
    throw new Error("비선호 행동 조회에 실패했습니다.");
  }

  return await response.json();
};

/* =========================================================
   비선호 행동 등록
========================================================= */

export const addDislike = async (actionId) => {
  const response = await authFetch(
    `${API_BASE_URL}/users/me/dislikes/${actionId}`,
    {
      method: "POST",
    },
  );

  if (!response.ok) {
    throw new Error("비선호 행동 저장에 실패했습니다.");
  }
};

/* =========================================================
   비선호 행동 해제
========================================================= */

export const removeDislike = async (actionId) => {
  const response = await authFetch(
    `${API_BASE_URL}/users/me/dislikes/${actionId}`,
    {
      method: "DELETE",
    },
  );

  if (!response.ok) {
    throw new Error("비선호 행동 해제에 실패했습니다.");
  }
};
