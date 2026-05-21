import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

export function RoleLayout({ roleName }) {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Hệ thống thi online</p>
          <h1>{roleName} Dashboard</h1>
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
          <NavLink to="dashboard">Tổng quan</NavLink>
        </aside>
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
