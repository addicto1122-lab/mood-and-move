import { useState } from "react";

import { Link, useNavigate } from "react-router-dom";

import { login, getMe, recoverAccount } from "../api/authApi";

import "./LoginPage.css";

export default function LoginPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [recoveryModalOpen, setRecoveryModalOpen] = useState(false);

  const [recovering, setRecovering] = useState(false);

  const moveAfterLogin = async () => {
    const user = await getMe();

    if (user.onboardingCompleted) {
      navigate("/", {
        replace: true
      });
    } else {
      navigate("/onboarding", {
        replace: true
      });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setIsSubmitting(true);

      const result = await login({
        email,
        password
      });

      // 탈퇴 신청된 계정
      if (result.withdrawalPending) {
        setRecoveryModalOpen(true);
        return;
      }

      // 일반 로그인
      await moveAfterLogin();
    } catch (error) {
      console.error(error);

      alert(error.message || "로그인에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRecoverAccount = async () => {
    try {
      setRecovering(true);

      await recoverAccount({
        email,
        password
      });

      setRecoveryModalOpen(false);

      await moveAfterLogin();
    } catch (error) {
      console.error(error);

      alert(error.message || "계정 복구에 실패했습니다.");
    } finally {
      setRecovering(false);
    }
  };

  const handleCancelRecovery = () => {
    if (recovering) {
      return;
    }

    setRecoveryModalOpen(false);
    setPassword("");
  };

  return (
    <main className="auth-page">
      <section className="auth-container">
        <header className="auth-header">
          <div className="auth-logo">
            Mood<span>&</span>Move
          </div>

          <h1>다시 만나서 반가워요</h1>

          <p>오늘의 마음을 기록하러 가볼까요?</p>
        </header>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="email">이메일</label>

            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="example@email.com"
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="password">비밀번호</label>

            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호를 입력해주세요"
              required
            />
          </div>

          <button className="auth-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? "로그인 중..." : "로그인"}
          </button>
        </form>

        <div className="auth-bottom">
          <span>아직 계정이 없나요?</span>

          <Link to="/signup">회원가입</Link>
        </div>
      </section>

      {/* 계정 복구 모달 */}
      {recoveryModalOpen && (
        <div className="recovery-modal-overlay">
          <div className="recovery-modal" role="dialog" aria-modal="true">
            <div className="recovery-modal-icon">↻</div>

            <h2>탈퇴 신청된 계정입니다</h2>

            <p>
              현재 회원탈퇴 신청이 진행 중인 계정입니다.
              <br />
              다시 이용하려면 계정을 복구해주세요.
            </p>

            <div className="recovery-modal-buttons">
              <button
                type="button"
                className="recovery-cancel"
                onClick={handleCancelRecovery}
                disabled={recovering}
              >
                취소
              </button>

              <button
                type="button"
                className="recovery-confirm"
                onClick={handleRecoverAccount}
                disabled={recovering}
              >
                {recovering ? "복구 중..." : "계정 복구"}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
