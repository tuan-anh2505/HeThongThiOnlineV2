import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, resolveErrorMessage } from "./teacherUtils.js";

export function TeacherClassesPage() {
  const [classes, setClasses] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadClasses() {
      setLoading(true);
      setError("");
      try {
        const data = await teacherService.getMyClasses();
        setClasses(data ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải danh sách lớp phụ trách"));
      } finally {
        setLoading(false);
      }
    }

    loadClasses();
  }, []);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Lớp phụ trách</h2>
        <p>Danh sách lớp học phần thuộc phạm vi giảng viên đang phụ trách.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Danh sách lớp</h3>
          <span className="muted-text">{loading ? "Đang tải..." : `${classes.length} lớp`}</span>
        </div>
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Mã lớp</th>
                <th>Tên lớp</th>
                <th>Số sinh viên</th>
                <th>Trạng thái</th>
                <th>Cập nhật</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {classes.map((schoolClass) => (
                <tr key={schoolClass.classId}>
                  <td>{schoolClass.classCode}</td>
                  <td>{schoolClass.className}</td>
                  <td>{schoolClass.studentCount}</td>
                  <td>
                    <span className={`status-pill ${schoolClass.status === "ACTIVE" ? "active" : "inactive"}`}>
                      {schoolClass.status}
                    </span>
                  </td>
                  <td>{formatDateTime(schoolClass.updatedAt)}</td>
                  <td>
                    <Link className="text-link" to={`/teacher/classes/${schoolClass.classId}/students`}>
                      Xem sinh viên
                    </Link>
                  </td>
                </tr>
              ))}
              {classes.length === 0 ? (
                <tr>
                  <td colSpan="6" className="empty-cell">
                    Chưa có lớp phụ trách.
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
