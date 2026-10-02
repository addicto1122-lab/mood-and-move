import { useState } from "react";

import { Link, useNavigate } from "react-router-dom";

import { login } from "../api/authApi";

import "./LoginPage.css";

export default function LoginPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");

  const [password, setPassword] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      await login({
        email,
        password
      });

      navigate("/", {
        replace: true
      });
    } catch (error) {
      alert(error.message);
    }
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

          <button className="auth-submit" type="submit">
            로그인
          </button>
        </form>

        <div className="auth-bottom">
          <span>아직 계정이 없나요?</span>

          <Link to="/signup">회원가입</Link>
        </div>
      </section>
    </main>
  );
}
