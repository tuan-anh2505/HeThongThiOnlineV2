import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { resolveErrorMessage } from "./studentUtils.js";

export function StudentSubjectsPage() {
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadSubjects() {
      setLoading(true);
      setError("");
      try {
        const data = await studentService.getSubjects();
        setSubjects(data ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải danh sách môn thi"));
      } finally {
        setLoading(false);
      }
    }

    loadSubjects();
  }, []);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Danh sách môn thi</h2>
        <p>Chỉ hiển thị các môn thuộc lớp sinh viên đang tham gia.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Môn thi của tôi</h3>
          <span className="muted-text">{loading ? "Đang tải..." : `${subjects.length} phân công`}</span>
        </div>
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Lớp</th>
                <th>Môn thi</th>
                <th>Giảng viên</th>
                <th>Học kỳ</th>
                <th>Năm học</th>
              </tr>
            </thead>
            <tbody>
              {subjects.map((item) => (
                <tr key={item.id}>
                  <td>{item.schoolClass?.className || item.classId}</td>
                  <td>{item.subject?.subjectName || item.subjectId}</td>
                  <td>{item.teacher?.account?.fullName || item.teacher?.teacherCode || item.teacherId}</td>
                  <td>{item.semester || "-"}</td>
                  <td>{item.schoolYear || "-"}</td>
                </tr>
              ))}
              {subjects.length === 0 ? (
                <tr>
                  <td colSpan="5" className="empty-cell">
                    Chưa có môn thi.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
        <div className="form-actions">
          <Link className="primary-link" to="/student/exams">
            Xem bài thi được giao
          </Link>
        </div>
      </section>
    </section>
  );
}
