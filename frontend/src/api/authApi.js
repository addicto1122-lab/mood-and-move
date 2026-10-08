const API_BASE_URL = "/api";

let handlingUnauthorized = false;

// Access Token 재발급
async function refreshAccessToken() {
  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    credentials: "include"
  });

  return response.ok;
}

// 인증 요청 공통 처리
export async function authFetch(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    credentials: "include"
  });

  if (response.status !== 401) {
    return response;
  }

  const refreshed = await refreshAccessToken();

  if (refreshed) {
    return fetch(url, {
      ...options,
      credentials: "include"
    });
  }

  if (!handlingUnauthorized) {
    handlingUnauthorized = true;

    alert(
      "로그인 세션이 만료되었거나 다른 기기에서 로그인되었습니다.\n다시 로그인해주세요."
    );

    window.location.href = "/login";
  }

  return response;
}

// 현재 위치 기반 추천 약관 조회
export async function getCurrentLocationPolicy() {
  const response = await fetch(`${API_BASE_URL}/consents/current-location`, {
    method: "GET"
  });

  if (!response.ok) {
    throw new Error("약관 정보를 불러오지 못했습니다.");
  }

  return response.json();
}

// 회원가입
export async function signup({
  email,
  password,
  nickname,
  locationPolicyId,
  locationConsent
}) {
  const response = await fetch(`${API_BASE_URL}/auth/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      email,
      password,
      nickname,
      locationPolicyId,
      locationConsent
    })
  });

  if (!response.ok) {
    throw new Error("회원가입에 실패했습니다.");
  }
}

// 이메일 중복확인
export async function checkEmail(email) {
  const response = await fetch(
    `${API_BASE_URL}/auth/check-email?email=${encodeURIComponent(email)}`,
    {
      method: "GET",
      credentials: "include"
    }
  );

  if (!response.ok) {
    throw new Error("이메일 중복확인에 실패했습니다.");
  }

  return response.json();
}

// 로그인
export async function login({ email, password }) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      email,
      password
    })
  });

  if (!response.ok) {
    throw new Error("이메일 또는 비밀번호가 올바르지 않습니다.");
  }

  return response.json();
}

// 탈퇴 신청 계정 복구
export async function recoverAccount({ email, password }) {
  const response = await fetch(`${API_BASE_URL}/auth/recover`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      email,
      password
    })
  });

  if (!response.ok) {
    if (response.status === 400) {
      throw new Error("계정 복구에 실패했습니다.");
    }

    throw new Error("계정 복구 중 오류가 발생했습니다.");
  }
}

// 로그아웃
export async function logout() {
  const response = await authFetch(`${API_BASE_URL}/auth/logout`, {
    method: "POST"
  });

  if (!response.ok) {
    throw new Error("로그아웃에 실패했습니다.");
  }
}

// 현재 로그인 사용자 조회
export async function getMe() {
  const response = await authFetch(`${API_BASE_URL}/auth/me`, {
    method: "GET"
  });

  if (!response.ok) {
    throw new Error("로그인이 필요합니다.");
  }

  return response.json();
}

// 비밀번호 변경
export async function changePassword({ currentPassword, newPassword }) {
  const response = await authFetch(`${API_BASE_URL}/users/me/password`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      currentPassword,
      newPassword
    })
  });

  if (!response.ok) {
    if (response.status === 400) {
      throw new Error("현재 비밀번호를 확인해주세요.");
    }

    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("비밀번호 변경에 실패했습니다.");
  }
}

// 온보딩 필수 정보 저장
export async function completeOnboarding({ ageGroup, gender, hobbyIds }) {
  const response = await authFetch(`${API_BASE_URL}/users/me/onboarding`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      ageGroup,
      gender,
      hobbyIds
    })
  });

  if (!response.ok) {
    if (response.status === 400) {
      throw new Error("필수 정보를 다시 확인해주세요.");
    }

    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("온보딩 저장에 실패했습니다.");
  }
}

// 전체 취미 목록 조회
export async function getHobbies() {
  const response = await authFetch(`${API_BASE_URL}/users/hobbies`, {
    method: "GET"
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("취미 목록을 불러오지 못했습니다.");
  }

  return response.json();
}

// 현재 취미 및 선호 설정 조회
export async function getPreferences() {
  const response = await authFetch(`${API_BASE_URL}/users/me/preferences`, {
    method: "GET"
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("선호 설정을 불러오지 못했습니다.");
  }

  return response.json();
}

// 취미 및 선호 설정 수정
export async function updatePreferences({
  hobbyIds,
  activityStyle,
  activityEnvironment,
  socialPreference,
  defaultAvailableMinutes,
  defaultRegionName,
  defaultRegionCode,
  defaultRegionLatitude,
  defaultRegionLongitude
}) {
  const response = await authFetch(`${API_BASE_URL}/users/me/preferences`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      hobbyIds,
      activityStyle,
      activityEnvironment,
      socialPreference,
      defaultAvailableMinutes,
      defaultRegionName,
      defaultRegionCode,
      defaultRegionLatitude,
      defaultRegionLongitude
    })
  });

  if (!response.ok) {
    if (response.status === 400) {
      throw new Error("선호 설정을 다시 확인해주세요.");
    }

    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("선호 설정 저장에 실패했습니다.");
  }
}

// 프로필 기본정보 수정
export async function updateProfile({ nickname, ageGroup, gender }) {
  const response = await authFetch(`${API_BASE_URL}/users/me/profile`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      nickname,
      ageGroup,
      gender
    })
  });

  if (!response.ok) {
    if (response.status === 400) {
      throw new Error("프로필 정보를 다시 확인해주세요.");
    }

    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("프로필 수정에 실패했습니다.");
  }
}

// 비선호 행동 목록 조회
export async function getDislikeActions() {
  const response = await authFetch(`${API_BASE_URL}/users/me/dislikes`, {
    method: "GET"
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("비선호 행동 목록을 불러오지 못했습니다.");
  }

  return response.json();
}

// 비선호 행동 추가
export async function addDislikeAction(actionId) {
  const response = await authFetch(
    `${API_BASE_URL}/users/me/dislikes/${actionId}`,
    {
      method: "POST"
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("비선호 행동 추가에 실패했습니다.");
  }
}

// 비선호 행동 삭제
export async function removeDislikeAction(actionId) {
  const response = await authFetch(
    `${API_BASE_URL}/users/me/dislikes/${actionId}`,
    {
      method: "DELETE"
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("비선호 행동 삭제에 실패했습니다.");
  }
}

// 회원탈퇴 신청
export async function requestWithdrawal(currentPassword) {
  const response = await authFetch(`${API_BASE_URL}/users/me/withdrawal`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      currentPassword
    })
  });

  if (!response.ok) {
    if (response.status === 400) {
      throw new Error(
        "비밀번호가 올바르지 않거나 이미 탈퇴 신청이 진행 중입니다."
      );
    }

    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("회원탈퇴 신청에 실패했습니다.");
  }
}

// 기본 활동 지역 검색
export async function searchRegions(query) {
  const response = await authFetch(
    `${API_BASE_URL}/locations/search?query=${encodeURIComponent(query)}`,
    {
      method: "GET"
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("지역 검색에 실패했습니다.");
  }

  return response.json();
}

// 현재 위치 기반 추천 동의 상태 조회
export async function getLocationConsent() {
  const response = await authFetch(
    `${API_BASE_URL}/consents/me/current-location`,
    {
      method: "GET"
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("위치정보 동의 상태를 불러오지 못했습니다.");
  }

  return response.json();
}

// 현재 위치 기반 추천 동의 상태 변경
export async function updateLocationConsent(agreed) {
  const response = await authFetch(
    `${API_BASE_URL}/consents/me/current-location`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        agreed
      })
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("위치정보 동의 상태 변경에 실패했습니다.");
  }
}

// 소셜 로그인 계정 복구
export async function recoverSocialAccount() {
  const response = await fetch(`${API_BASE_URL}/auth/social/recover`, {
    method: "POST",
    credentials: "include"
  });

  if (!response.ok) {
    throw new Error("계정 복구에 실패했습니다. 다시 로그인해주세요.");
  }
}

// 소셜 로그인 계정 복구 취소
export async function cancelSocialRecovery() {
  const response = await fetch(`${API_BASE_URL}/auth/social/recover/cancel`, {
    method: "POST",
    credentials: "include"
  });

  if (!response.ok) {
    throw new Error("계정 복구 취소에 실패했습니다.");
  }
}
