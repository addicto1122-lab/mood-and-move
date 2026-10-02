import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMe } from "../api/authApi";
import LogoutButton from "../components/LogoutButton";
import "./MyPage.css";

export default function MyPage() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchUser() {
      try {
        const data = await getMe();
        setUser(data);
      } catch (error) {
        console.error(error);
        navigate("/login", {
          replace: true
        });
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, [navigate]);

  if (loading) {
    return (
      <main className="mypage">
        <div className="mypage-loading">불러오는 중...</div>
      </main>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <main className="mypage">
      <section className="mypage-container">
        {/* 제목 */}
        <h1 className="mypage-title">마이페이지</h1>

        {/* 프로필 */}
        <section className="mypage-profile-card">
          <div className="mypage-profile-left">
            <div className="mypage-profile-avatar">
              {user.nickname?.charAt(0) || "M"}
            </div>

            <div className="mypage-profile-info">
              <strong>{user.nickname}</strong>

              <span>{user.email}</span>
            </div>
          </div>

          <button
            type="button"
            className="mypage-profile-edit"
            onClick={() => navigate("/mypage/profile")}
          >
            프로필 수정
          </button>
        </section>

        {/* 나의 설정 */}
        <section className="mypage-settings-section">
          <h2>나의 설정</h2>

          <div className="mypage-settings-card">
            {/* 취미 및 선호 */}
            <button
              type="button"
              className="mypage-settings-item"
              onClick={() => navigate("/mypage/preferences")}
            >
              <div className="mypage-settings-icon">♡</div>

              <div className="mypage-settings-text">
                <strong>취미 및 선호 설정</strong>

                <span>나에게 맞는 활동 추천을 위한 설정</span>
              </div>

              <span className="mypage-settings-arrow">›</span>
            </button>

            <div className="mypage-settings-divider" />

            {/* 비선호 행동 */}
            <button
              type="button"
              className="mypage-settings-item"
              onClick={() => {
                // 추후 비선호 행동 관리 페이지 연결
              }}
            >
              <div className="mypage-settings-icon">◎</div>

              <div className="mypage-settings-text">
                <strong>비선호 행동 관리</strong>

                <span>추천에서 제외하고 싶은 행동 관리</span>
              </div>

              <span className="mypage-settings-arrow">›</span>
            </button>
          </div>
        </section>

        {/* 로그아웃 */}
        <div className="mypage-logout">
          <LogoutButton />
        </div>

        {/* 하단 문구 */}
        <footer className="mypage-footer">
          Mood&amp;Move v1.0 · 오늘도 내 마음을 가볍게
        </footer>
      </section>
    </main>
  );
}
