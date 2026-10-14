import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import {
  sendPasswordResetCode,
  verifyPasswordResetCode,
  confirmPasswordReset
} from "../api/authApi";

import "./ForgotPasswordPage.css";

export default function ForgotPasswordPage() {
  const [step, setStep] = useState("email");

  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [resetToken, setResetToken] = useState(null);

  const [newPassword, setNewPassword] = useState("");
  const [passwordCheck, setPasswordCheck] = useState("");

  const [cooldown, setCooldown] = useState(0);
  const [expiresIn, setExpiresIn] = useState(0);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // 이메일 인증 타이머
  useEffect(() => {
    const timer = setInterval(() => {
      setCooldown((prev) => Math.max(0, prev - 1));
      setExpiresIn((prev) => Math.max(0, prev - 1));
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const remaining = seconds % 60;

    return `${minutes}:${String(remaining).padStart(2, "0")}`;
  };

  // 1. 인증번호 발송
  const handleSendCode = async (e) => {
    e?.preventDefault();

    if (loading || cooldown > 0) return;

    const normalizedEmail = email.trim().toLowerCase();

    if (!normalizedEmail) {
      setError("이메일을 입력해주세요.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await sendPasswordResetCode(normalizedEmail);

      setEmail(normalizedEmail);
      setCode("");
      setResetToken(null);

      setCooldown(60);
      setExpiresIn(300);

      setStep("verify");
    } catch (err) {
      setError(err.message || "인증번호 발송에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 2. 인증번호 검증
  const handleVerifyCode = async (e) => {
    e.preventDefault();

    if (loading) return;

    if (expiresIn <= 0) {
      setError("인증번호가 만료되었습니다. 재발송해주세요.");
      return;
    }

    if (!/^\d{6}$/.test(code)) {
      setError("인증번호 6자리를 입력해주세요.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const result = await verifyPasswordResetCode(email, code);

      if (!result.resetToken) {
        throw new Error("재설정 토큰을 받지 못했습니다.");
      }

      setResetToken(result.resetToken);
      setCode("");
      setStep("reset");
    } catch (err) {
      setError(err.message || "이메일 인증에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 3. 새 비밀번호 설정
  const handleResetPassword = async (e) => {
    e.preventDefault();

    if (loading) return;

    if (newPassword.length < 8 || newPassword.length > 50) {
      setError("비밀번호는 8자 이상 50자 이하로 입력해주세요.");
      return;
    }

    if (newPassword !== passwordCheck) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }

    if (!resetToken) {
      setError("인증 정보가 없습니다. 다시 인증해주세요.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await confirmPasswordReset(resetToken, newPassword);

      setResetToken(null);
      setNewPassword("");
      setPasswordCheck("");
      setStep("complete");
    } catch (err) {
      setError(err.message || "비밀번호 변경에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 이메일 변경 시 인증 상태 초기화
  const handleChangeEmail = () => {
    setStep("email");
    setCode("");
    setResetToken(null);
    setNewPassword("");
    setPasswordCheck("");
    setCooldown(0);
    setExpiresIn(0);
    setError("");
  };

  return (
    <main className="forgot-page">
      <section className="forgot-container">
        <header className="forgot-header">
          <div className="forgot-logo">
            Mood<span>&</span>Move
          </div>

          <h1>
            {step === "complete" ? "비밀번호 변경 완료" : "비밀번호 찾기"}
          </h1>

          <p>
            {step === "email" &&
              "가입한 이메일을 입력하면 인증번호를 보내드려요."}

            {step === "verify" &&
              "이메일로 받은 6자리 인증번호를 입력해주세요."}

            {step === "reset" && "사용할 새로운 비밀번호를 입력해주세요."}

            {step === "complete" && "새 비밀번호로 다시 로그인해주세요."}
          </p>
        </header>

        {step === "email" && (
          <form className="forgot-form" onSubmit={handleSendCode}>
            <div className="forgot-field">
              <label htmlFor="forgot-email">이메일</label>

              <input
                id="forgot-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@email.com"
                autoComplete="email"
                disabled={loading}
                required
              />
            </div>

            <button type="submit" className="forgot-submit" disabled={loading}>
              {loading ? "발송 중..." : "인증번호 발송"}
            </button>
          </form>
        )}

        {step === "verify" && (
          <form className="forgot-form" onSubmit={handleVerifyCode}>
            <div className="forgot-email-info">
              <span>{email}</span>

              <button
                type="button"
                onClick={handleChangeEmail}
                disabled={loading}
              >
                변경
              </button>
            </div>

            <div className="forgot-field">
              <label htmlFor="forgot-code">인증번호</label>

              <input
                id="forgot-code"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={code}
                onChange={(e) =>
                  setCode(e.target.value.replace(/\D/g, "").slice(0, 6))
                }
                placeholder="6자리 인증번호"
                maxLength={6}
                disabled={loading}
                required
              />
            </div>

            <p className="forgot-timer">
              {expiresIn > 0
                ? `인증번호 유효시간 ${formatTime(expiresIn)}`
                : "인증번호가 만료되었습니다."}
            </p>

            <button
              type="submit"
              className="forgot-submit"
              disabled={loading || code.length !== 6 || expiresIn <= 0}
            >
              {loading ? "확인 중..." : "인증번호 확인"}
            </button>

            <button
              type="button"
              className="forgot-secondary"
              onClick={handleSendCode}
              disabled={loading || cooldown > 0}
            >
              {cooldown > 0 ? `재발송까지 ${cooldown}초` : "인증번호 재발송"}
            </button>
          </form>
        )}

        {step === "reset" && (
          <form className="forgot-form" onSubmit={handleResetPassword}>
            <div className="forgot-field">
              <label htmlFor="new-password">새 비밀번호</label>

              <input
                id="new-password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="8~50자 입력해주세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={50}
                disabled={loading}
                required
              />
            </div>

            <div className="forgot-field">
              <label htmlFor="password-confirm">새 비밀번호 확인</label>

              <input
                id="password-confirm"
                type="password"
                value={passwordCheck}
                onChange={(e) => setPasswordCheck(e.target.value)}
                placeholder="비밀번호를 다시 입력해주세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={50}
                disabled={loading}
                required
              />

              {passwordCheck && (
                <p
                  className={
                    newPassword === passwordCheck
                      ? "forgot-match"
                      : "forgot-error"
                  }
                >
                  {newPassword === passwordCheck
                    ? "비밀번호가 일치합니다."
                    : "비밀번호가 일치하지 않습니다."}
                </p>
              )}
            </div>

            <button type="submit" className="forgot-submit" disabled={loading}>
              {loading ? "변경 중..." : "비밀번호 변경"}
            </button>
          </form>
        )}

        {step === "complete" && (
          <div className="forgot-complete">
            <div className="forgot-complete-icon">✓</div>

            <p>비밀번호가 성공적으로 변경되었습니다.</p>

            <Link to="/login" className="forgot-submit">
              로그인하러 가기
            </Link>
          </div>
        )}

        {error && (
          <p className="forgot-error" role="alert">
            {error}
          </p>
        )}

        {step !== "complete" && (
          <div className="forgot-bottom">
            <Link to="/login">← 로그인으로 돌아가기</Link>
          </div>
        )}
      </section>
    </main>
  );
}
