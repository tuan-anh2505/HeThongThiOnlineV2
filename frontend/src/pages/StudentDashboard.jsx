import { useAuth } from "../context/AuthContext.jsx";

export function StudentDashboard() {
  const { user } = useAuth();

  return (
    <section className="dashboard-section">
      <h2>Trang sinh viên</h2>
      <p>Xin chào {user?.fullName || user?.username}. Bạn có thể xem môn thi, bài thi được giao, bắt đầu làm bài và xem kết quả khi được công bố.</p>
      <div className="dashboard-grid">
        <article>
          <h3>Bài thi của tôi</h3>
          <p>Theo dõi bài thi đã công bố và trạng thái làm bài.</p>
        </article>
        <article>
          <h3>Làm bài</h3>
          <p>Bắt đầu bài thi, lưu câu trả lời tự động và nộp bài.</p>
        </article>
        <article>
          <h3>Kết quả</h3>
          <p>Xem điểm và review bài làm khi giảng viên cho phép.</p>
        </article>
      </div>
    </section>
  );
}
