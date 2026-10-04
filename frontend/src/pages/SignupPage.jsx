import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  checkEmail,
  signup,
  login,
  getCurrentLocationPolicy
} from "../api/authApi";
import "./SignupPage.css";

export default function SignupPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailAvailable, setEmailAvailable] = useState(null);

  const [nickname, setNickname] = useState("");

  const [password, setPassword] = useState("");
  const [passwordCheck, setPasswordCheck] = useState("");

  // 현재 위치 기반 추천 약관
  const [locationPolicy, setLocationPolicy] = useState(null);
  const [locationConsent, setLocationConsent] = useState(false);
  const [policyError, setPolicyError] = useState("");

  /*
   * 현재 활성화된 위치 기반 추천 약관 조회
   */
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

  const checkEmailDuplicate = async () => {
    if (!email.trim()) {
      alert("이메일을 입력해주세요.");
      return;
    }

    try {
      const data = await checkEmail(email);

      setEmailChecked(true);
      setEmailAvailable(data.available);
    } catch (error) {
      alert(error.message);
    }
  };

  const handleEmailChange = (e) => {
    setEmail(e.target.value);

    // 이메일을 수정하면 중복확인 다시 해야 함
    setEmailChecked(false);
    setEmailAvailable(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!emailChecked || !emailAvailable) {
      alert("이메일 중복확인을 해주세요.");
      return;
    }

    if (password.length < 8) {
      alert("비밀번호는 8자 이상 입력해주세요.");
      return;
    }

    if (password !== passwordCheck) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    /*
     * 사용자가 실제로 확인한 약관 ID를
     * 회원가입 요청에 포함해야 하므로
     * 약관 조회가 끝나지 않았다면 가입하지 않음
     */
    if (!locationPolicy) {
      alert("약관 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
      return;
    }

    try {
      // 회원가입
      await signup({
        email,
        password,
        nickname,
        locationPolicyId: locationPolicy.id,
        locationConsent
      });

      // 회원가입 직후 자동 로그인
      await login({
        email,
        password
      });

      // JWT 쿠키가 저장된 상태로 온보딩 이동
      navigate("/onboarding");
    } catch (error) {
      alert(error.message);
    }
  };

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
                required
              />

              <button
                type="button"
                className="email-check-button"
                onClick={checkEmailDuplicate}
              >
                중복확인
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

          {/* 현재 위치 기반 추천 선택 약관 */}
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
            disabled={!locationPolicy}
          >
            회원가입
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
