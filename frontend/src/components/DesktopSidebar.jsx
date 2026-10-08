import { NavLink } from "react-router-dom";
import LogoutButton from "./LogoutButton";
import RecordNavLink from "../constants/RecordNavLink";

export default function DesktopSidebar() {
  return (
    <aside className="desktop-sidebar">
      <div className="sidebar-logo">
        Mood<span>&</span>Move
      </div>

      <nav className="sidebar-menu">
        <NavLink to="/">홈</NavLink>

        <NavLink to="/calendar">캘린더</NavLink>

        <RecordNavLink />

        <NavLink to="/stats">통계</NavLink>

        <NavLink to="/mypage">마이페이지</NavLink>
      </nav>

      <div className="desktop-logout">
        <LogoutButton />
      </div>
    </aside>
  );
}
