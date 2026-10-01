import { NavLink } from "react-router-dom";

export default function BottomNav() {
  return (
    <nav className="bottom-nav">
      <NavLink to="/">홈</NavLink>
      <NavLink to="/mood">기록</NavLink>
      <NavLink to="/stats">통계</NavLink>
      <NavLink to="/mypage">마이</NavLink>
    </nav>
  );
}
