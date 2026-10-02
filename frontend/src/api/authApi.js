const API_BASE_URL = "/api";

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

export async function logout() {
  const response = await fetch(`${API_BASE_URL}/auth/logout`, {
    method: "POST",
    credentials: "include"
  });

  if (!response.ok) {
    throw new Error("로그아웃에 실패했습니다.");
  }
}

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

export async function updateNickname(nickname) {
  const response = await fetch("/api/users/me/nickname", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      nickname
    })
  });

  if (!response.ok) {
    throw new Error("닉네임 수정에 실패했습니다.");
  }
}

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
