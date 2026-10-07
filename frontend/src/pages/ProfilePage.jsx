import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  getMe,
  updateProfile,
  changePassword,
  getLocationConsent,
  updateLocationConsent
} from "../api/authApi";

import { AGE_GROUP_OPTIONS, GENDER_OPTIONS } from "../constants/userOptions";

import "./ProfilePage.css";

export default function ProfilePage() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  /*
   * 프로필
   */
  const [nickname, setNickname] = useState("");
  const [ageGroup, setAgeGroup] = useState("");
  const [gender, setGender] = useState("");

  const [profileError, setProfileError] = useState("");
  const [savingProfile, setSavingProfile] = useState(false);

  /*
   * 위치정보 약관 동의
   */
  const [locationPolicy, setLocationPolicy] = useState(null);

  const [locationConsent, setLocationConsent] = useState(false);

  const [savedLocationConsent, setSavedLocationConsent] = useState(false);

  const [consentError, setConsentError] = useState("");

  const [savingConsent, setSavingConsent] = useState(false);

  /*
   * 비밀번호
   */
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");

  const [passwordError, setPasswordError] = useState("");
  const [savingPassword, setSavingPassword] = useState(false);

  /*
   * 토스트
   */
  const [toast, setToast] = useState("");

  /*
   * 현재 사용자 정보 + 위치 약관 동의 상태 조회
   */
  useEffect(() => {
    async function fetchUser() {
      try {
        const [userData, consentData] = await Promise.all([
          getMe(),
          getLocationConsent()
        ]);

        /*
         * 사용자
         */
        setUser(userData);

        setNickname(userData.nickname ?? "");
        setAgeGroup(userData.ageGroup ?? "");
        setGender(userData.gender ?? "");

        /*
         * 위치 약관
         */
        setLocationPolicy(consentData);

        setLocationConsent(consentData.agreed ?? false);

        setSavedLocationConsent(consentData.agreed ?? false);
      } catch (error) {
        console.error(error);

        if (error.message === "로그인이 필요합니다.") {
          navigate("/login", {
            replace: true
          });

          return;
        }

        setLoadError(error.message || "프로필 정보를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, [navigate]);

  /*
   * 토스트
   */
  const showToast = (message) => {
    setToast(message);

    setTimeout(() => {
      setToast("");
    }, 2000);
  };

  /*
   * 프로필 수정
   */
  const handleProfileUpdate = async (e) => {
    e.preventDefault();

    setProfileError("");

    const trimmedNickname = nickname.trim();

    if (!trimmedNickname) {
      setProfileError("닉네임을 입력해주세요.");
      return;
    }

    if (trimmedNickname.length > 50) {
      setProfileError("닉네임은 50자 이하로 입력해주세요.");

      return;
    }

    if (!ageGroup) {
      setProfileError("나이대를 선택해주세요.");
      return;
    }

    if (!gender) {
      setProfileError("성별을 선택해주세요.");
      return;
    }

    const unchanged =
      trimmedNickname === user.nickname &&
      ageGroup === user.ageGroup &&
      gender === user.gender;

    if (unchanged) {
      setProfileError("변경된 정보가 없습니다.");
      return;
    }

    try {
      setSavingProfile(true);

      await updateProfile({
        nickname: trimmedNickname,
        ageGroup,
        gender
      });

      setUser((prev) => ({
        ...prev,
        nickname: trimmedNickname,
        ageGroup,
        gender
      }));

      setNickname(trimmedNickname);

      showToast("프로필이 수정되었습니다.");
    } catch (error) {
      console.error(error);

      setProfileError(error.message || "프로필 수정에 실패했습니다.");
    } finally {
      setSavingProfile(false);
    }
  };

  /*
   * 위치정보 약관 동의 변경
   */
  const handleLocationConsentUpdate = async () => {
    setConsentError("");

    if (locationConsent === savedLocationConsent) {
      setConsentError("변경된 위치정보 동의 설정이 없습니다.");

      return;
    }

    try {
      setSavingConsent(true);

      await updateLocationConsent(locationConsent);

      setSavedLocationConsent(locationConsent);

      showToast(
        locationConsent
          ? "현재 위치 기반 추천에 동의했습니다."
          : "현재 위치 기반 추천 동의를 철회했습니다."
      );
    } catch (error) {
      console.error(error);

      setConsentError(
        error.message || "위치정보 동의 설정 변경에 실패했습니다."
      );
    } finally {
      setSavingConsent(false);
    }
  };

  /*
   * 비밀번호 변경
   */
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
        navigate("/login", {
          replace: true
        });
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

  if (loadError) {
    return (
      <main className="profile-page">
        <div className="profile-page-loading">{loadError}</div>
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

        {/* =========================
            기본 정보
        ========================= */}

        <section className="profile-section">
          <h2>기본 정보</h2>

          <form
            className="profile-card profile-info-card"
            onSubmit={handleProfileUpdate}
          >
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
            <div className="profile-field">
              <label htmlFor="profile-nickname">닉네임</label>

              <input
                id="profile-nickname"
                type="text"
                value={nickname}
                maxLength={50}
                onChange={(e) => {
                  setNickname(e.target.value);

                  setProfileError("");
                }}
              />
            </div>

            <div className="profile-divider" />

            {/* 나이대 */}
            <div className="profile-field">
              <label>나이대</label>

              <div className="profile-age-grid">
                {AGE_GROUP_OPTIONS.map((item) => (
                  <button
                    key={item.value}
                    type="button"
                    className={
                      ageGroup === item.value
                        ? "profile-choice selected"
                        : "profile-choice"
                    }
                    onClick={() => {
                      setAgeGroup(item.value);

                      setProfileError("");
                    }}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="profile-divider" />

            {/* 성별 */}
            <div className="profile-field">
              <label>성별</label>

              <div className="profile-gender-grid">
                {GENDER_OPTIONS.map((item) => (
                  <button
                    key={item.value}
                    type="button"
                    className={
                      gender === item.value
                        ? "profile-choice selected"
                        : "profile-choice"
                    }
                    onClick={() => {
                      setGender(item.value);

                      setProfileError("");
                    }}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            {profileError && (
              <span className="profile-error">{profileError}</span>
            )}

            <button
              type="submit"
              className="profile-save-button"
              disabled={savingProfile}
            >
              {savingProfile ? "저장 중..." : "프로필 저장"}
            </button>
          </form>
        </section>

        {/* =========================
            현재 위치 기반 추천
        ========================= */}

        {locationPolicy && (
          <section className="profile-section">
            <h2>현재 위치 기반 추천</h2>

            <div className="profile-card profile-consent-card">
              <div className="profile-consent-header">
                <div>
                  <strong>{locationPolicy.title}</strong>

                  <span>{locationPolicy.required ? "필수" : "선택"}</span>
                </div>

                <button
                  type="button"
                  className={
                    locationConsent
                      ? "profile-consent-toggle active"
                      : "profile-consent-toggle"
                  }
                  onClick={() => {
                    setLocationConsent((prev) => !prev);

                    setConsentError("");
                  }}
                  aria-pressed={locationConsent}
                >
                  <span />
                </button>
              </div>

              <p className="profile-consent-content">
                {locationPolicy.content}
              </p>

              <div className="profile-consent-status">
                현재 상태:
                <strong>{locationConsent ? " 동의" : " 미동의"}</strong>
              </div>

              {consentError && (
                <span className="profile-error">{consentError}</span>
              )}

              <button
                type="button"
                className="profile-save-button"
                onClick={handleLocationConsentUpdate}
                disabled={
                  savingConsent || locationConsent === savedLocationConsent
                }
              >
                {savingConsent ? "저장 중..." : "동의 설정 저장"}
              </button>
            </div>
          </section>
        )}

        {/* =========================
            비밀번호
        ========================= */}

        {user.localLoginEnabled && (
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
        )}
      </section>

      {toast && <div className="profile-toast">{toast}</div>}
    </main>
  );
}
