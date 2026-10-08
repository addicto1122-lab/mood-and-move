import { NavLink } from "react-router-dom";

import LogoutButton from "./LogoutButton";

export default function DesktopSidebar() {
  return (
    <aside className="desktop-sidebar">
      <div className="sidebar-logo">
        Mood<span>&</span>Move
      </div>

      <nav className="sidebar-menu">
        <NavLink to="/">홈</NavLink>

        <NavLink to="/calendar">캘린더</NavLink>

        <NavLink to="/mood">기록</NavLink>

        <NavLink to="/stats">통계</NavLink>

        <NavLink to="/mypage">마이페이지</NavLink>
      </nav>

      <div className="desktop-logout">
        <LogoutButton />
      </div>
    </aside>
  );
}
