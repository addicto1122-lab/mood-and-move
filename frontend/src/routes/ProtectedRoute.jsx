import { useEffect, useState } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { getMe } from "../api/authApi";

export default function ProtectedRoute() {
  const [isLoggedIn, setIsLoggedIn] = useState(null);

  useEffect(() => {
    async function checkAuth() {
      try {
        await getMe();
        setIsLoggedIn(true);
      } catch {
        setIsLoggedIn(false);
      }
    }

    checkAuth();
  }, []);

  // 인증 확인 중
  if (isLoggedIn === null) {
    return null;
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
