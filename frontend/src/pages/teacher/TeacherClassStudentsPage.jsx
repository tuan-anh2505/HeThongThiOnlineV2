import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, getClassLabel, resolveErrorMessage } from "./teacherUtils.js";

export function TeacherClassStudentsPage() {
  const { classId: routeClassId } = useParams();
  const [classes, setClasses] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState(routeClassId ?? "");
  const [students, setStudents] = useState([]);
  const [studentId, setStudentId] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const selectedClass = useMemo(
    () => classes.find((schoolClass) => schoolClass.classId === selectedClassId),
    [classes, selectedClassId]
  );

  useEffect(() => {
    async function loadClasses() {
      setError("");
      try {
        const data = await teacherService.getMyClasses();
        setClasses(data ?? []);
        if (!selectedClassId && data?.[0]?.classId) {
          setSelectedClassId(data[0].classId);
        }
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải danh sách lớp"));
      }
    }

    loadClasses();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    async function loadStudents() {
      if (!selectedClassId) {
        setStudents([]);
        return;
      }

      setLoading(true);
      setError("");
      try {
        const data = await teacherService.getClassStudents(selectedClassId);
        setStudents(data ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải sinh viên trong lớp"));
      } finally {
        setLoading(false);
      }
    }

    loadStudents();
  }, [selectedClassId]);

  const reloadStudents = async () => {
    const data = await teacherService.getClassStudents(selectedClassId);
    setStudents(data ?? []);
  };

  const handleAddStudent = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!selectedClassId || !studentId.trim()) {
      setError("Vui lòng chọn lớp và nhập studentId");
      return;
    }

    setSaving(true);
    try {
      await teacherService.addStudentToClass(selectedClassId, studentId);
      setMessage("Đã thêm sinh viên vào lớp");
      setStudentId("");
      await reloadStudents();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể thêm sinh viên vào lớp"));
    } finally {
      setSaving(false);
    }
  };

  const handleRemoveStudent = async (student) => {
    if (!window.confirm(`Bạn chắc chắn muốn xóa sinh viên ${student.studentCode} khỏi lớp?`)) {
      return;
    }

    setError("");
    setMessage("");
    try {
      await teacherService.removeStudentFromClass(selectedClassId, student.studentId);
      setMessage("Đã xóa sinh viên khỏi lớp");
      await reloadStudents();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể xóa sinh viên khỏi lớp"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Sinh viên trong lớp</h2>
        <p>Xem danh sách sinh viên thuộc lớp phụ trách. Nếu được phân quyền, giảng viên có thể thêm hoặc xóa sinh viên.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="form-grid">
          <label>
            Lớp
            <select value={selectedClassId} onChange={(event) => setSelectedClassId(event.target.value)}>
              <option value="">Chọn lớp</option>
              {classes.map((schoolClass) => (
                <option key={schoolClass.classId} value={schoolClass.classId}>
                  {getClassLabel(schoolClass)}
                </option>
              ))}
            </select>
          </label>
          <div className="read-only-box">
            <span>Số sinh viên</span>
            <strong>{selectedClass?.studentCount ?? students.length}</strong>
          </div>
        </div>
      </section>

      <div className="admin-two-column">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Danh sách sinh viên</h3>
            <span className="muted-text">{loading ? "Đang tải..." : `${students.length} sinh viên`}</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã sinh viên</th>
                  <th>Họ tên</th>
                  <th>Email</th>
                  <th>Ngày vào lớp</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {students.map((student) => (
                  <tr key={student.id}>
                    <td>{student.studentCode}</td>
                    <td>{student.account?.fullName || "-"}</td>
                    <td>{student.account?.email || "-"}</td>
                    <td>{formatDateTime(student.joinedAt)}</td>
                    <td>
                      <span className={`status-pill ${student.status === "ACTIVE" ? "active" : "inactive"}`}>
                        {student.status}
                      </span>
                    </td>
                    <td>
                      <button className="danger-text-button" type="button" onClick={() => handleRemoveStudent(student)}>
                        Xóa khỏi lớp
                      </button>
                    </td>
                  </tr>
                ))}
                {students.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty-cell">
                      Chưa có sinh viên trong lớp.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <h3>Thêm sinh viên</h3>
          <form className="stacked-form" onSubmit={handleAddStudent}>
            <label>
              Student ID
              <input value={studentId} onChange={(event) => setStudentId(event.target.value)} />
            </label>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? "Đang thêm..." : "Thêm vào lớp"}
            </button>
          </form>
          <p className="muted-text">Nhập studentId của hồ sơ sinh viên đã có trong hệ thống.</p>
        </section>
      </div>
    </section>
  );
}
