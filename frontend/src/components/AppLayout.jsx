import { Outlet } from "react-router-dom";
import BottomNav from "./BottomNav.jsx";
import DesktopSidebar from "./DesktopSidebar.jsx";

export default function AppLayout() {
  return (
    <div className="app-shell">
      <DesktopSidebar />

      <div className="app-content">
        <Outlet />
      </div>

      <BottomNav />
    </div>
  );
}
