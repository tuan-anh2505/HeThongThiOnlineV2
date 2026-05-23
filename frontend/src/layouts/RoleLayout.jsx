import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
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
    { to: "/admin/statistics", label: "Thống kê" }
  ],
  TEACHER: [
    { to: "/teacher/dashboard", label: "Tổng quan" },
    { to: "/teacher/classes", label: "Lớp phụ trách" },
    { to: "/teacher/class-students", label: "Sinh viên trong lớp" },
    { to: "/teacher/question-banks", label: "Ngân hàng câu hỏi" },
    { to: "/teacher/questions", label: "Câu hỏi" },
    { to: "/teacher/exams", label: "Bài thi và ca thi" },
    { to: "/teacher/attempts", label: "Bài làm" },
    { to: "/teacher/statistics", label: "Thống kê điểm" }
  ],
  STUDENT: [{ to: "/student/dashboard", label: "Tổng quan" }]
};

function getInitial(user) {
  const source = user?.fullName || user?.username || user?.email || "U";
  return source.trim().charAt(0).toUpperCase();
}

function getAvatarUrl(user) {
  return user?.avatarUrl || user?.profileImageUrl || user?.imageUrl || "";
}

export function RoleLayout({ roleName, title }) {
  const { user, logout } = useAuth();
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const displayRoleName = roleName || ROLE_LABELS[user?.role] || "Người dùng";
  const heading = title || `${displayRoleName} Dashboard`;
  const navItems = NAV_ITEMS[user?.role] ?? [];
  const avatarUrl = getAvatarUrl(user);

  useEffect(() => {
    function handlePointerDown(event) {
      if (!menuRef.current?.contains(event.target)) {
        setProfileMenuOpen(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, []);

  const closeProfileMenu = () => setProfileMenuOpen(false);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Hệ thống thi online</p>
          <h1>{heading}</h1>
        </div>
        <div className="user-area">
          <span>{user?.fullName || user?.username}</span>
          <div className="profile-menu" ref={menuRef}>
            <button
              className="avatar-button"
              type="button"
              aria-haspopup="menu"
              aria-expanded={profileMenuOpen}
              onClick={() => setProfileMenuOpen((current) => !current)}
            >
              {avatarUrl ? <img src={avatarUrl} alt="Ảnh đại diện" /> : <span>{getInitial(user)}</span>}
            </button>

            {profileMenuOpen ? (
              <div className="profile-dropdown" role="menu">
                <div className="profile-dropdown-header">
                  <strong>{user?.fullName || user?.username}</strong>
                  <span>{user?.email || user?.username}</span>
                  <small>{user?.role}</small>
                </div>
                <Link to="/profile" role="menuitem" onClick={closeProfileMenu}>
                  Hồ sơ tài khoản
                </Link>
                <Link to="/profile?tab=change-password" role="menuitem" onClick={closeProfileMenu}>
                  Đổi mật khẩu
                </Link>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    closeProfileMenu();
                    logout();
                  }}
                >
                  Đăng xuất
                </button>
              </div>
            ) : null}
          </div>
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
