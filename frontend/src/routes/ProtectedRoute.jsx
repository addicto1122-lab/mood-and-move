import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";

import { getMe } from "../api/authApi";

export default function ProtectedRoute() {
  const location = useLocation();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [checkedPath, setCheckedPath] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function checkAuth() {
      setLoading(true);

      try {
        const data = await getMe();

        if (!cancelled) {
          setUser(data);
          setCheckedPath(location.pathname);
        }
      } catch (error) {
        console.error(error);

        if (!cancelled) {
          setUser(null);
          setCheckedPath(location.pathname);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    checkAuth();

    return () => {
      cancelled = true;
    };
  }, [location.pathname]);

  // 현재 경로에 대한 인증 확인이 끝날 때까지 대기
  if (loading || checkedPath !== location.pathname) {
    return null;
  }

  // 로그인 안 됨
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // 필수 온보딩 미완료
  if (!user.onboardingCompleted && location.pathname !== "/onboarding") {
    return <Navigate to="/onboarding" replace />;
  }

  // 이미 온보딩 완료
  if (user.onboardingCompleted && location.pathname === "/onboarding") {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
