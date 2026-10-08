import { NavLink } from "react-router-dom";
import RecordNavLink from "../constants/RecordNavLink";

export default function BottomNav() {
  return (
    <nav className="bottom-nav">
      <NavLink to="/">홈</NavLink>

      <NavLink to="/calendar">캘린더</NavLink>

      <RecordNavLink />

      <NavLink to="/stats">통계</NavLink>

      <NavLink to="/mypage">마이</NavLink>
    </nav>
  );
}
