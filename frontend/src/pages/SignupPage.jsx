import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./SignupPage.css";

export default function SignupPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailAvailable, setEmailAvailable] = useState(null);

  const [nickname, setNickname] = useState("");

  const [password, setPassword] = useState("");
  const [passwordCheck, setPasswordCheck] = useState("");

  const checkEmailDuplicate = async () => {
    if (!email.trim()) {
      alert("이메일을 입력해주세요.");
      return;
    }

    // TODO: 백엔드 API 연결
    // 예시
    //
    // const response = await fetch(
    //   `/api/auth/check-email?email=${encodeURIComponent(email)}`
    // );
    //
    // const data = await response.json();
    //
    // setEmailChecked(true);
    // setEmailAvailable(data.available);

    // UI 테스트용 임시값
    // 백api추가시 수정
    const available = true;

    setEmailChecked(true);
    setEmailAvailable(available);
  };

  const handleEmailChange = (e) => {
    setEmail(e.target.value);

    // 이메일을 수정하면 중복확인 다시 해야 함
    setEmailChecked(false);
    setEmailAvailable(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!emailChecked || !emailAvailable) {
      alert("이메일 중복확인을 해주세요.");
      return;
    }

    if (password !== passwordCheck) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    const signupData = {
      email,
      password,
      nickname
    };

    console.log(signupData);

    // TODO: 회원가입 API 연결
    //
    // fetch("/api/auth/signup", {
    //   method: "POST",
    //   headers: {
    //     "Content-Type": "application/json",
    //   },
    //   body: JSON.stringify(signupData),
    // });

    navigate("/onboarding");
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
              placeholder="비밀번호를 입력해주세요"
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

          <button className="signup-submit" type="submit">
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
