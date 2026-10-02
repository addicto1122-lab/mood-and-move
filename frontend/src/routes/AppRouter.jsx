import { BrowserRouter, Routes, Route } from "react-router-dom";

import ProtectedRoute from "./ProtectedRoute.jsx";
import AppLayout from "../components/AppLayout.jsx";

import HomePage from "../pages/HomePage.jsx";
import LoginPage from "../pages/LoginPage.jsx";
import SignupPage from "../pages/SignupPage.jsx";
import OnboardingPage from "../pages/OnboardingPage.jsx";
import MoodWritePage from "../pages/MoodWritePage.jsx";
import RecommendationPage from "../pages/RecommendationPage.jsx";
import StatsPage from "../pages/StatsPage.jsx";
import MyPage from "../pages/MyPage.jsx";
import ProfilePage from "../pages/ProfilePage.jsx";

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 로그인 없이 접근 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        {/* 로그인 필요 */}
        <Route element={<ProtectedRoute />}>
          <Route path="/onboarding" element={<OnboardingPage />} />

          <Route element={<AppLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/mood" element={<MoodWritePage />} />
            <Route path="/recommendation" element={<RecommendationPage />} />
            <Route path="/stats" element={<StatsPage />} />
            <Route path="/mypage" element={<MyPage />} />
            <Route path="/mypage/profile" element={<ProfilePage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
