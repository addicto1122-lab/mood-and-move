import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Toaster } from "react-hot-toast";

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
import PreferencePage from "../pages/PreferencePage.jsx";
import DislikePage from "../pages/DislikePage.jsx";
import AccountRecoveryPage from "../pages/AccountRecoveryPage.jsx";
import CalendarPage from "../pages/CalendarPage.jsx";
import MoodRecheckPage from "../pages/MoodRecheckPage.jsx";
import MoodRouteGuard from "./MoodRouteGuard.jsx";

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Toaster
        position="top-center"
        reverseOrder={false}
        toastOptions={{ duration: 2000 }}
      />

      <Routes>
        {/* 로그인 없이 접근 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/account-recovery" element={<AccountRecoveryPage />} />

        {/* 로그인 필요 */}
        <Route element={<ProtectedRoute />}>
          <Route path="/onboarding" element={<OnboardingPage />} />

          <Route element={<AppLayout />}>
            <Route path="/" element={<HomePage />} />

            {/* 행동 실행 후 감정 재측정 */}
            <Route
              path="/action-executions/:executionId/recheck"
              element={<MoodRecheckPage />}
            />

            {/* 감정 일기 작성 */}
            <Route element={<MoodRouteGuard />}>
              <Route path="/mood" element={<MoodWritePage />} />
            </Route>

            <Route path="/recommendation" element={<RecommendationPage />} />
            <Route path="/stats" element={<StatsPage />} />
            <Route path="/mypage" element={<MyPage />} />
            <Route path="/mypage/profile" element={<ProfilePage />} />
            <Route path="/mypage/preferences" element={<PreferencePage />} />
            <Route path="/mypage/dislikes" element={<DislikePage />} />
            <Route path="/calendar" element={<CalendarPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
