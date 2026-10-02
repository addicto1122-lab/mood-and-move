const API_BASE_URL = "/api";

// 회원가입
export async function signup({ email, password, nickname }) {
  const response = await fetch(`${API_BASE_URL}/auth/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      email,
      password,
      nickname
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
}

// 로그아웃
export async function logout() {
  const response = await fetch(`${API_BASE_URL}/auth/logout`, {
    method: "POST",
    credentials: "include"
  });

  if (!response.ok) {
    throw new Error("로그아웃에 실패했습니다.");
  }
}

// 현재 로그인 사용자 조회
export async function getMe() {
  const response = await fetch(`${API_BASE_URL}/auth/me`, {
    method: "GET",
    credentials: "include"
  });

  if (!response.ok) {
    throw new Error("로그인이 필요합니다.");
  }

  return response.json();
}

// 비밀번호 변경
export async function changePassword({ currentPassword, newPassword }) {
  const response = await fetch(`${API_BASE_URL}/users/me/password`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
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
  const response = await fetch(`${API_BASE_URL}/users/me/onboarding`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
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
  const response = await fetch(`${API_BASE_URL}/users/hobbies`, {
    method: "GET",
    credentials: "include"
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
  const response = await fetch(`${API_BASE_URL}/users/me/preferences`, {
    method: "GET",
    credentials: "include"
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
  defaultAvailableMinutes
}) {
  const response = await fetch(`${API_BASE_URL}/users/me/preferences`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      hobbyIds,
      activityStyle,
      activityEnvironment,
      socialPreference,
      defaultAvailableMinutes
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
  const response = await fetch(`${API_BASE_URL}/users/me/profile`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
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

export async function getDislikeActions() {
  const response = await fetch(`${API_BASE_URL}/users/me/dislikes`, {
    method: "GET",
    credentials: "include"
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("비선호 행동 목록을 불러오지 못했습니다.");
  }

  return response.json();
}

export async function addDislikeAction(actionId) {
  const response = await fetch(
    `${API_BASE_URL}/users/me/dislikes/${actionId}`,
    {
      method: "POST",
      credentials: "include"
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("비선호 행동 추가에 실패했습니다.");
  }
}

//비선호행동
export async function removeDislikeAction(actionId) {
  const response = await fetch(
    `${API_BASE_URL}/users/me/dislikes/${actionId}`,
    {
      method: "DELETE",
      credentials: "include"
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("로그인이 필요합니다.");
    }

    throw new Error("비선호 행동 삭제에 실패했습니다.");
  }
}
