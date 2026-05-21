import { useAuth } from "../context/AuthContext.jsx";

export function TeacherDashboard() {
  const { user } = useAuth();

  return (
    <section className="dashboard-section">
      <h2>Trang giảng viên</h2>
      <p>Xin chào {user?.fullName || user?.username}. Bạn có thể quản lý ngân hàng câu hỏi, bài thi, ca thi và xem thống kê điểm.</p>
      <div className="dashboard-grid">
        <article>
          <h3>Ngân hàng câu hỏi</h3>
          <p>Tạo câu hỏi thủ công, import từ file hoặc từ đường link.</p>
        </article>
        <article>
          <h3>Bài thi</h3>
          <p>Tạo đề, công bố bài thi, theo dõi attempt và công bố kết quả.</p>
        </article>
        <article>
          <h3>Thống kê</h3>
          <p>Xem điểm theo bài thi, lớp và môn phụ trách.</p>
        </article>
      </div>
    </section>
  );
}
