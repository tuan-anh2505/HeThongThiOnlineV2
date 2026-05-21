import { useAuth } from "../context/AuthContext.jsx";

export function AdminDashboard() {
  const { user } = useAuth();

  return (
    <section className="dashboard-section">
      <h2>Trang quản trị</h2>
      <p>Xin chào {user?.fullName || user?.username}. Bạn có quyền quản lý tài khoản, lớp, môn thi, bài thi và nhật ký hệ thống.</p>
      <div className="dashboard-grid">
        <article>
          <h3>Tài khoản</h3>
          <p>Quản lý tài khoản và phân quyền người dùng.</p>
        </article>
        <article>
          <h3>Dữ liệu đào tạo</h3>
          <p>Quản lý lớp, môn thi và phân công giảng viên.</p>
        </article>
        <article>
          <h3>Giám sát</h3>
          <p>Theo dõi bài thi, kết quả, thống kê và nhật ký hệ thống.</p>
        </article>
      </div>
    </section>
  );
}
