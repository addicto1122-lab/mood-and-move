import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMe, updateNickname, changePassword } from "../api/authApi";
import "./ProfilePage.css";

export default function ProfilePage() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // 닉네임
  const [nickname, setNickname] = useState("");
  const [nicknameError, setNicknameError] = useState("");
  const [savingNickname, setSavingNickname] = useState(false);

  // 비밀번호
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [savingPassword, setSavingPassword] = useState(false);

  // 토스트
  const [toast, setToast] = useState("");

  useEffect(() => {
    async function fetchUser() {
      try {
        const data = await getMe();

        setUser(data);
        setNickname(data.nickname);
      } catch (error) {
        console.error(error);
        navigate("/login", { replace: true });
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, [navigate]);

  const showToast = (message) => {
    setToast(message);

    setTimeout(() => {
      setToast("");
    }, 2000);
  };

  const handleNicknameUpdate = async (e) => {
    e.preventDefault();

    const value = nickname.trim();

    if (!value) {
      setNicknameError("닉네임을 입력해주세요.");
      return;
    }

    if (value.length > 50) {
      setNicknameError("닉네임은 50자 이하로 입력해주세요.");
      return;
    }

    if (value === user.nickname) {
      setNicknameError("현재 닉네임과 동일합니다.");
      return;
    }

    try {
      setSavingNickname(true);
      setNicknameError("");

      await updateNickname(value);

      setUser((prev) => ({
        ...prev,
        nickname: value
      }));

      showToast("닉네임이 수정되었습니다.");
    } catch (error) {
      console.error(error);
      setNicknameError("닉네임 수정에 실패했습니다.");
    } finally {
      setSavingNickname(false);
    }
  };

  const handlePasswordUpdate = async (e) => {
    e.preventDefault();

    setPasswordError("");

    if (!currentPassword) {
      setPasswordError("현재 비밀번호를 입력해주세요.");
      return;
    }

    if (!newPassword) {
      setPasswordError("새 비밀번호를 입력해주세요.");
      return;
    }

    if (newPassword.length < 8) {
      setPasswordError("새 비밀번호는 8자 이상 입력해주세요.");
      return;
    }

    if (newPassword.length > 50) {
      setPasswordError("새 비밀번호는 50자 이하로 입력해주세요.");
      return;
    }

    if (newPassword !== newPasswordConfirm) {
      setPasswordError("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    if (currentPassword === newPassword) {
      setPasswordError("현재 비밀번호와 다른 비밀번호를 입력해주세요.");
      return;
    }

    try {
      setSavingPassword(true);

      await changePassword({
        currentPassword,
        newPassword
      });

      setCurrentPassword("");
      setNewPassword("");
      setNewPasswordConfirm("");

      showToast("비밀번호가 변경되었습니다. 다시 로그인해주세요.");

      setTimeout(() => {
        navigate("/login", { replace: true });
      }, 1800);
    } catch (error) {
      console.error(error);

      setPasswordError(error.message || "비밀번호 변경에 실패했습니다.");
    } finally {
      setSavingPassword(false);
    }
  };

  if (loading) {
    return (
      <main className="profile-page">
        <div className="profile-page-loading">불러오는 중...</div>
      </main>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <main className="profile-page">
      <section className="profile-page-container">
        {/* 헤더 */}
        <header className="profile-page-header">
          <button
            type="button"
            className="profile-back-button"
            onClick={() => navigate("/mypage")}
            aria-label="마이페이지로 돌아가기"
          >
            ‹
          </button>

          <div>
            <h1>프로필 수정</h1>
            <p>내 계정 정보를 확인하고 수정할 수 있어요.</p>
          </div>
        </header>

        {/* 기본 정보 */}
        <section className="profile-section">
          <h2>기본 정보</h2>

          <div className="profile-card">
            {/* 이메일 */}
            <div className="profile-field">
              <label htmlFor="profile-email">이메일</label>

              <input
                id="profile-email"
                type="email"
                value={user.email}
                disabled
              />

              <span className="profile-help">이메일은 변경할 수 없습니다.</span>
            </div>

            <div className="profile-divider" />

            {/* 닉네임 */}
            <form onSubmit={handleNicknameUpdate}>
              <div className="profile-field">
                <label htmlFor="profile-nickname">닉네임</label>

                <div className="profile-input-button">
                  <input
                    id="profile-nickname"
                    type="text"
                    value={nickname}
                    maxLength={50}
                    onChange={(e) => {
                      setNickname(e.target.value);
                      setNicknameError("");
                    }}
                  />

                  <button type="submit" disabled={savingNickname}>
                    {savingNickname ? "저장 중" : "변경"}
                  </button>
                </div>

                {nicknameError && (
                  <span className="profile-error">{nicknameError}</span>
                )}
              </div>
            </form>
          </div>
        </section>

        {/* 비밀번호 */}
        <section className="profile-section">
          <h2>비밀번호 변경</h2>

          <form
            className="profile-card profile-password-card"
            onSubmit={handlePasswordUpdate}
          >
            <div className="profile-field">
              <label htmlFor="current-password">현재 비밀번호</label>

              <input
                id="current-password"
                type="password"
                value={currentPassword}
                autoComplete="current-password"
                placeholder="현재 비밀번호를 입력해주세요"
                onChange={(e) => {
                  setCurrentPassword(e.target.value);
                  setPasswordError("");
                }}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="new-password">새 비밀번호</label>

              <input
                id="new-password"
                type="password"
                value={newPassword}
                autoComplete="new-password"
                placeholder="8자 이상 입력해주세요"
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  setPasswordError("");
                }}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="new-password-confirm">새 비밀번호 확인</label>

              <input
                id="new-password-confirm"
                type="password"
                value={newPasswordConfirm}
                autoComplete="new-password"
                placeholder="새 비밀번호를 다시 입력해주세요"
                onChange={(e) => {
                  setNewPasswordConfirm(e.target.value);
                  setPasswordError("");
                }}
              />
            </div>

            {passwordError && (
              <span className="profile-error">{passwordError}</span>
            )}

            <button
              type="submit"
              className="profile-password-button"
              disabled={savingPassword}
            >
              {savingPassword ? "변경 중..." : "비밀번호 변경"}
            </button>
          </form>
        </section>
      </section>

      {/* 토스트 */}
      {toast && <div className="profile-toast">{toast}</div>}
    </main>
  );
}
