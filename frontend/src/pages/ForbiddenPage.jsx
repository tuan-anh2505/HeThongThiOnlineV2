import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { getHomePathForRole } from "../services/authService.js";

export function ForbiddenPage() {
  const { user } = useAuth();
  const homePath = user ? getHomePathForRole(user.role) : "/login";

  return (
    <main className="state-page">
      <h1>Không có quyền truy cập</h1>
      <p>Tài khoản hiện tại không có quyền mở trang này.</p>
      <Link className="primary-link" to={homePath}>
        Quay lại trang chính
      </Link>
    </main>
  );
}
