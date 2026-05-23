import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { formatDateTime, resolveErrorMessage } from "./studentUtils.js";

export function StudentExamsPage() {
  const [exams, setExams] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const filteredExams = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) {
      return exams;
    }
    return exams.filter((exam) =>
      [exam.examName, exam.subjectName, exam.className, exam.status, exam.attemptStatus]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    );
  }, [exams, query]);

  useEffect(() => {
    async function loadExams() {
      setLoading(true);
      setError("");
      try {
        const data = await studentService.getExams();
        setExams(data ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải danh sách bài thi"));
      } finally {
        setLoading(false);
      }
    }

    loadExams();
  }, []);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Danh sách bài thi được giao</h2>
        <p>Chỉ hiển thị bài thi đã công bố hoặc đang mở thuộc lớp sinh viên đang tham gia.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <form className="filter-form">
          <label>
            Tìm kiếm
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên bài thi, môn, lớp" />
          </label>
        </form>
      </section>

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Bài thi của tôi</h3>
          <span className="muted-text">{loading ? "Đang tải..." : `${filteredExams.length} bài thi`}</span>
        </div>
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Bài thi</th>
                <th>Môn</th>
                <th>Lớp</th>
                <th>Thời gian</th>
                <th>Ca thi</th>
                <th>Trạng thái</th>
                <th>Lần làm</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filteredExams.map((exam) => (
                <tr key={exam.examId}>
                  <td>
                    {exam.examName}
                    {exam.hasPassword ? <span className="inline-note">Yêu cầu mật khẩu</span> : null}
                  </td>
                  <td>{exam.subjectName}</td>
                  <td>{exam.className}</td>
                  <td>{exam.durationMinutes} phút</td>
                  <td>
                    {formatDateTime(exam.startTime)} - {formatDateTime(exam.endTime)}
                  </td>
                  <td>
                    <span className={`status-pill ${["PUBLISHED", "OPEN"].includes(exam.status) ? "active" : "inactive"}`}>
                      {exam.status}
                    </span>
                  </td>
                  <td>
                    {exam.attemptCount} / {exam.maxAttempts}
                    {exam.attemptStatus ? <span className="inline-note">{exam.attemptStatus}</span> : null}
                  </td>
                  <td>
                    <Link className="text-link" to={`/student/exams/${exam.examId}`}>
                      Chi tiết
                    </Link>
                  </td>
                </tr>
              ))}
              {filteredExams.length === 0 ? (
                <tr>
                  <td colSpan="8" className="empty-cell">
                    Chưa có bài thi được giao.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}
