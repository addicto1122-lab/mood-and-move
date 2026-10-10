import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  checkEmail,
  signup,
  login,
  getCurrentLocationPolicy,
  sendEmailCode,
  verifyEmailCode
} from "../api/authApi";

import "./SignupPage.css";

export default function SignupPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailAvailable, setEmailAvailable] = useState(null);
  const [checkingEmail, setCheckingEmail] = useState(false);

  // 이메일 인증
  const [emailCode, setEmailCode] = useState("");
  const [emailSent, setEmailSent] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [signupToken, setSignupToken] = useState("");

  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [expiresIn, setExpiresIn] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [passwordCheck, setPasswordCheck] = useState("");

  // 현재 위치 기반 추천 약관
  const [locationPolicy, setLocationPolicy] = useState(null);
  const [locationConsent, setLocationConsent] = useState(false);
  const [policyError, setPolicyError] = useState("");

  // 약관 조회
  useEffect(() => {
    async function fetchLocationPolicy() {
      try {
        const data = await getCurrentLocationPolicy();
        setLocationPolicy(data);
      } catch (error) {
        console.error(error);
        setPolicyError(error.message || "약관 정보를 불러오지 못했습니다.");
      }
    }

    fetchLocationPolicy();
  }, []);

  // 인증번호 재발송 대기시간 / 유효시간
  useEffect(() => {
    const timer = setInterval(() => {
      setCooldown((prev) => Math.max(0, prev - 1));
      setExpiresIn((prev) => Math.max(0, prev - 1));
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const formatTime = (seconds) => {
    const minutes = String(Math.floor(seconds / 60)).padStart(2, "0");
    const remaining = String(seconds % 60).padStart(2, "0");
    return `${minutes}:${remaining}`;
  };

  const normalizedEmail = email.trim().toLowerCase();

  // 이메일 변경 시 모든 인증 상태 초기화
  const handleEmailChange = (e) => {
    setEmail(e.target.value);

    setEmailChecked(false);
    setEmailAvailable(null);

    setEmailCode("");
    setEmailSent(false);
    setEmailVerified(false);
    setSignupToken("");

    setCooldown(0);
    setExpiresIn(0);
  };

  // 이메일 중복확인
  const checkEmailDuplicate = async () => {
    if (!normalizedEmail) {
      alert("이메일을 입력해주세요.");
      return;
    }

    setCheckingEmail(true);

    try {
      const data = await checkEmail(normalizedEmail);

      setEmailChecked(true);
      setEmailAvailable(data.available);

      if (!data.available) {
        alert("이미 사용 중인 이메일입니다.");
      }
    } catch (error) {
      alert(error.message || "이메일 중복확인에 실패했습니다.");
    } finally {
      setCheckingEmail(false);
    }
  };

  // 인증번호 발송
  const handleSendCode = async () => {
    if (!emailChecked || !emailAvailable) {
      alert("이메일 중복확인을 먼저 해주세요.");
      return;
    }

    if (cooldown > 0 || sendingCode) {
      return;
    }

    setSendingCode(true);

    try {
      await sendEmailCode(normalizedEmail);

      setEmailSent(true);
      setEmailVerified(false);
      setSignupToken("");
      setEmailCode("");

      setExpiresIn(300);
      setCooldown(60);

      alert("인증번호를 이메일로 발송했습니다.");
    } catch (error) {
      alert(error.message || "인증번호 발송에 실패했습니다.");
    } finally {
      setSendingCode(false);
    }
  };

  // 인증번호 입력
  const handleCodeChange = (e) => {
    const value = e.target.value.replace(/\D/g, "").slice(0, 6);
    setEmailCode(value);
  };

  // 인증번호 검증
  const handleVerifyCode = async () => {
    if (emailCode.length !== 6) {
      alert("6자리 인증번호를 입력해주세요.");
      return;
    }

    if (expiresIn <= 0) {
      alert("인증번호가 만료되었습니다. 다시 발송해주세요.");
      return;
    }

    setVerifyingCode(true);

    try {
      const data = await verifyEmailCode(normalizedEmail, emailCode);

      if (!data.signupToken) {
        throw new Error("가입용 인증 토큰을 받지 못했습니다.");
      }

      setSignupToken(data.signupToken);
      setEmailVerified(true);
      setEmailCode("");

      alert("이메일 인증이 완료되었습니다.");
    } catch (error) {
      alert(error.message || "이메일 인증에 실패했습니다.");
    } finally {
      setVerifyingCode(false);
    }
  };

  // 회원가입
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (submitting) return;

    if (!emailChecked || !emailAvailable) {
      alert("이메일 중복확인을 해주세요.");
      return;
    }

    if (!emailVerified || !signupToken) {
      alert("이메일 인증을 완료해주세요.");
      return;
    }

    if (password.length < 8 || password.length > 50) {
      alert("비밀번호는 8자 이상 50자 이하로 입력해주세요.");
      return;
    }

    if (password !== passwordCheck) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    if (!locationPolicy) {
      alert("약관 정보를 불러오지 못했습니다.");
      return;
    }

    setSubmitting(true);

    // 회원가입
    try {
      await signup({
        email: normalizedEmail,
        password,
        nickname,
        locationPolicyId: locationPolicy.id,
        locationConsent,
        signupToken
      });
    } catch (error) {
      alert(error.message || "회원가입에 실패했습니다.");
      setSubmitting(false);
      return;
    }

    // 회원가입 성공 후 자동 로그인
    try {
      await login({
        email: normalizedEmail,
        password
      });

      navigate("/onboarding");
    } catch (error) {
      console.error(error);
      alert(
        "회원가입은 완료되었지만 자동 로그인에 실패했습니다. 직접 로그인해주세요."
      );

      navigate("/login");
    } finally {
      setSubmitting(false);
    }
  };

  const emailBusy = checkingEmail || sendingCode || verifyingCode;

  return (
    <main className="signup-page">
      <section className="signup-container">
        <header className="signup-header">
          <div className="signup-logo">
            Mood<span>&</span>Move
          </div>

          <h1>처음 만나서 반가워요</h1>

          <p>
            간단한 정보만 입력하면
            <br />
            바로 시작할 수 있어요.
          </p>
        </header>

        <form className="signup-form" onSubmit={handleSubmit}>
          {/* 이메일 */}
          <div className="signup-field">
            <label htmlFor="email">이메일</label>

            <div className="email-check-row">
              <input
                id="email"
                type="email"
                value={email}
                onChange={handleEmailChange}
                placeholder="example@email.com"
                disabled={emailBusy || submitting}
                required
              />

              <button
                type="button"
                className="email-check-button"
                onClick={checkEmailDuplicate}
                disabled={emailBusy || submitting}
              >
                {checkingEmail ? "확인 중..." : "중복확인"}
              </button>
            </div>

            {emailChecked && emailAvailable && (
              <p className="check-message success">사용 가능한 이메일입니다.</p>
            )}

            {emailChecked && emailAvailable === false && (
              <p className="check-message error">
                이미 사용 중인 이메일입니다.
              </p>
            )}
          </div>

          {/* 이메일 인증 */}
          <div className="signup-field email-verification">
            <label>이메일 인증</label>

            {emailVerified ? (
              <div className="email-verified-box">
                <span>✓</span>
                이메일 인증이 완료되었습니다.
              </div>
            ) : (
              <>
                <button
                  type="button"
                  className="email-send-button"
                  onClick={handleSendCode}
                  disabled={
                    !emailChecked ||
                    !emailAvailable ||
                    sendingCode ||
                    verifyingCode ||
                    cooldown > 0
                  }
                >
                  {sendingCode
                    ? "발송 중..."
                    : cooldown > 0
                      ? `재발송 ${cooldown}초`
                      : emailSent
                        ? "인증번호 재발송"
                        : "인증번호 발송"}
                </button>

                {emailSent && (
                  <>
                    <div className="email-code-row">
                      <input
                        type="text"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        value={emailCode}
                        onChange={handleCodeChange}
                        placeholder="6자리 인증번호"
                        maxLength={6}
                        disabled={verifyingCode || submitting}
                      />

                      <button
                        type="button"
                        className="email-verify-button"
                        onClick={handleVerifyCode}
                        disabled={
                          emailCode.length !== 6 ||
                          expiresIn <= 0 ||
                          verifyingCode
                        }
                      >
                        {verifyingCode ? "확인 중..." : "인증확인"}
                      </button>
                    </div>

                    {expiresIn > 0 ? (
                      <p className="email-timer">
                        인증번호 유효시간 {formatTime(expiresIn)}
                      </p>
                    ) : (
                      <p className="check-message error">
                        인증번호가 만료되었습니다. 재발송해주세요.
                      </p>
                    )}
                  </>
                )}
              </>
            )}
          </div>

          {/* 닉네임 */}
          <div className="signup-field">
            <label htmlFor="nickname">닉네임</label>

            <input
              id="nickname"
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="사용할 닉네임을 입력해주세요"
              maxLength={50}
              required
            />
          </div>

          {/* 비밀번호 */}
          <div className="signup-field">
            <label htmlFor="password">비밀번호</label>

            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="8자 이상 입력해주세요"
              minLength={8}
              maxLength={50}
              required
            />
          </div>

          {/* 비밀번호 확인 */}
          <div className="signup-field">
            <label htmlFor="password-check">비밀번호 확인</label>

            <input
              id="password-check"
              type="password"
              value={passwordCheck}
              onChange={(e) => setPasswordCheck(e.target.value)}
              placeholder="비밀번호를 다시 입력해주세요"
              minLength={8}
              required
            />

            {passwordCheck && password !== passwordCheck && (
              <p className="check-message error">
                비밀번호가 일치하지 않습니다.
              </p>
            )}

            {passwordCheck && password === passwordCheck && (
              <p className="check-message success">비밀번호가 일치합니다.</p>
            )}
          </div>

          {/* 위치정보 약관 */}
          {locationPolicy && (
            <div className="signup-consent">
              <label className="signup-consent-label">
                <input
                  type="checkbox"
                  checked={locationConsent}
                  onChange={(e) => setLocationConsent(e.target.checked)}
                />

                <span>
                  <strong>
                    {locationPolicy.required ? "[필수]" : "[선택]"}
                  </strong>{" "}
                  {locationPolicy.title}
                </span>
              </label>

              <p className="signup-consent-description">
                {locationPolicy.content}
              </p>
            </div>
          )}

          {policyError && <p className="check-message error">{policyError}</p>}

          <button
            className="signup-submit"
            type="submit"
            disabled={
              !locationPolicy || !emailVerified || !signupToken || submitting
            }
          >
            {submitting ? "가입 처리 중..." : "회원가입"}
          </button>
        </form>

        <div className="signup-bottom">
          <span>이미 계정이 있나요?</span>
          <Link to="/login">로그인</Link>
        </div>
      </section>
    </main>
  );
}
