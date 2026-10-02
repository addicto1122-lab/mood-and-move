import LogoutButton from "../components/LogoutButton";

import "./MyPage.css";

export default function MyPage() {
  return (
    <main className="mypage">
      <h1>마이페이지</h1>

      {/* 이후 프로필 / 설정 영역 추가 */}

      <div className="mobile-logout">
        <LogoutButton />
      </div>
    </main>
  );
}
