import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { getHomePathForRole } from "../services/authService.js";

export function NotFoundPage() {
  const { user } = useAuth();
  const homePath = user ? getHomePathForRole(user.role) : "/login";

  return (
    <main className="state-page">
      <h1>Không tìm thấy trang</h1>
      <p>Đường dẫn bạn mở không tồn tại trong hệ thống.</p>
      <Link className="primary-link" to={homePath}>
        Quay lại trang chính
      </Link>
    </main>
  );
}
