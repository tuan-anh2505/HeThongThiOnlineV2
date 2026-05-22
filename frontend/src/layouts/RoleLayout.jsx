import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { getHomePathForRole } from "../services/authService.js";

const ROLE_LABELS = {
  ADMIN: "Admin",
  TEACHER: "Giảng viên",
  STUDENT: "Sinh viên"
};

export function RoleLayout({ roleName, title }) {
  const { user, logout } = useAuth();
  const displayRoleName = roleName || ROLE_LABELS[user?.role] || "Người dùng";
  const heading = title || `${displayRoleName} Dashboard`;

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Hệ thống thi online</p>
          <h1>{heading}</h1>
        </div>
        <div className="user-area">
          <span>{user?.fullName || user?.username}</span>
          <button className="secondary-button" type="button" onClick={logout}>
            Đăng xuất
          </button>
        </div>
      </header>

      <div className="content-frame">
        <aside className="sidebar">
          <NavLink to={getHomePathForRole(user?.role)}>Tổng quan</NavLink>
          <NavLink to="/profile">Hồ sơ</NavLink>
        </aside>
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
