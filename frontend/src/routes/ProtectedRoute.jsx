import { Navigate, Outlet } from "react-router-dom";

export default function ProtectedRoute() {
  // 지금은 프론트 구현 확인용 임시값
  const isLoggedIn = true;

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
