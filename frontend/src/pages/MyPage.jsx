import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMe, requestWithdrawal } from "../api/authApi";

import LogoutButton from "../components/LogoutButton";
import "./MyPage.css";

export default function MyPage() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false);
  const [withdrawPassword, setWithdrawPassword] = useState("");
  const [withdrawError, setWithdrawError] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);

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

  const openWithdrawModal = () => {
    setWithdrawPassword("");
    setWithdrawError("");
    setWithdrawModalOpen(true);
  };

  const closeWithdrawModal = () => {
    if (withdrawing) {
      return;
    }

    setWithdrawPassword("");
    setWithdrawError("");
    setWithdrawModalOpen(false);
  };

  const handleWithdrawal = async (e) => {
    e.preventDefault();

    if (user.localLoginEnabled && !withdrawPassword.trim()) {
      setWithdrawError("현재 비밀번호를 입력해주세요.");

      return;
    }

    try {
      setWithdrawing(true);
      setWithdrawError("");

      await requestWithdrawal(user.localLoginEnabled ? withdrawPassword : null);

      navigate("/login", {
        replace: true
      });
    } catch (error) {
      console.error(error);

      setWithdrawError(error.message || "회원탈퇴 신청에 실패했습니다.");
    } finally {
      setWithdrawing(false);
    }
  };

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
              onClick={() => navigate("/mypage/dislikes")}
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

        {/* 회원탈퇴 */}
        <button
          type="button"
          className="mypage-withdraw-button"
          onClick={openWithdrawModal}
        >
          회원탈퇴
        </button>

        {/* 하단 문구 */}
        <footer className="mypage-footer">
          Mood&amp;Move v1.0 · 오늘도 내 마음을 가볍게
        </footer>
      </section>

      {/* 회원탈퇴 신청 모달 */}
      {withdrawModalOpen && (
        <div className="mypage-modal-overlay">
          <div
            className="mypage-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="withdraw-title"
          >
            <h2 id="withdraw-title">회원탈퇴 신청</h2>

            <p className="mypage-modal-description">
              탈퇴 신청 후 7일 동안 계정이 유지됩니다.
              <br />
              7일 안에 다시 로그인하면 계정을 복구할 수 있습니다.
              <br />
              {user.localLoginEnabled
                ? "계속하려면 현재 비밀번호를 입력해주세요."
                : "계속하려면 탈퇴 신청 버튼을 눌러주세요."}
            </p>

            <form onSubmit={handleWithdrawal}>
              {user.localLoginEnabled && (
                <input
                  type="password"
                  value={withdrawPassword}
                  placeholder="현재 비밀번호"
                  autoComplete="current-password"
                  onChange={(e) => {
                    setWithdrawPassword(e.target.value);

                    setWithdrawError("");
                  }}
                />
              )}

              {withdrawError && (
                <span className="mypage-modal-error">{withdrawError}</span>
              )}

              <div className="mypage-modal-buttons">
                <button
                  type="button"
                  className="mypage-modal-cancel"
                  onClick={closeWithdrawModal}
                  disabled={withdrawing}
                >
                  취소
                </button>

                <button
                  type="submit"
                  className="mypage-modal-delete"
                  disabled={withdrawing}
                >
                  {withdrawing ? "신청 중..." : "탈퇴 신청"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </main>
  );
}
