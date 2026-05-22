import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

const ROLE_LABELS = {
  ADMIN: "Admin",
  TEACHER: "Giảng viên",
  STUDENT: "Sinh viên"
};

const NAV_ITEMS = {
  ADMIN: [
    { to: "/admin/dashboard", label: "Tổng quan" },
    { to: "/admin/accounts", label: "Tài khoản" },
    { to: "/admin/classes", label: "Lớp" },
    { to: "/admin/class-students", label: "Sinh viên trong lớp" },
    { to: "/admin/subjects", label: "Môn thi" },
    { to: "/admin/assignments", label: "Phân công" },
    { to: "/admin/question-banks", label: "Ngân hàng câu hỏi" },
    { to: "/admin/logs", label: "Nhật ký hệ thống" },
    { to: "/admin/statistics", label: "Thống kê" },
    { to: "/profile", label: "Hồ sơ" }
  ],
  TEACHER: [
    { to: "/teacher/dashboard", label: "Tổng quan" },
    { to: "/profile", label: "Hồ sơ" }
  ],
  STUDENT: [
    { to: "/student/dashboard", label: "Tổng quan" },
    { to: "/profile", label: "Hồ sơ" }
  ]
};

export function RoleLayout({ roleName, title }) {
  const { user, logout } = useAuth();
  const displayRoleName = roleName || ROLE_LABELS[user?.role] || "Người dùng";
  const heading = title || `${displayRoleName} Dashboard`;
  const navItems = NAV_ITEMS[user?.role] ?? [];

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
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </aside>
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
