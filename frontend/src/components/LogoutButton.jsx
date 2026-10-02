import { useNavigate } from "react-router-dom";

import { logout } from "../api/authApi";

import "./LogoutButton.css";

export default function LogoutButton({ className = "" }) {
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();

      navigate("/login", {
        replace: true
      });
    } catch (error) {
      alert(error.message);
    }
  };

  return (
    <button
      type="button"
      className={`logout-button ${className}`}
      onClick={handleLogout}
    >
      <span className="logout-icon">↪</span>

      <span>로그아웃</span>
    </button>
  );
}
