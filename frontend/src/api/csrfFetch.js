const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

let csrfRequestPromise = null;

// 브라우저 쿠키에서 CSRF 토큰 읽기
function readCsrfToken() {
  const cookie = document.cookie
    .split("; ")
    .find((item) => item.startsWith(`${CSRF_COOKIE_NAME}=`));

  if (!cookie) {
    return null;
  }

  return decodeURIComponent(cookie.substring(CSRF_COOKIE_NAME.length + 1));
}

// CSRF 토큰 확보
async function getCsrfToken() {
  const existingToken = readCsrfToken();

  if (existingToken) {
    return existingToken;
  }

  // 여러 요청이 동시에 발생해도 토큰 발급 요청은 공유
  if (!csrfRequestPromise) {
    csrfRequestPromise = fetch("/api/csrf", {
      method: "GET",
      credentials: "include",
      cache: "no-store"
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("CSRF 토큰 발급에 실패했습니다.");
        }

        const token = readCsrfToken();

        if (!token) {
          throw new Error("CSRF 토큰 쿠키를 찾을 수 없습니다.");
        }

        return token;
      })
      .finally(() => {
        csrfRequestPromise = null;
      });
  }

  return csrfRequestPromise;
}

// CSRF 보호가 적용된 공통 fetch
export async function csrfFetch(url, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = new Headers(options.headers);

  if (!SAFE_METHODS.has(method)) {
    const csrfToken = await getCsrfToken();

    headers.set(CSRF_HEADER_NAME, csrfToken);
  }

  return fetch(url, {
    ...options,
    headers,
    credentials: "include"
  });
}
