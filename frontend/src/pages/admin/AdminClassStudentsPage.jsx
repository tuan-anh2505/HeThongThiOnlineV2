import { useEffect, useMemo, useState } from "react";
import { adminService } from "../../services/adminService.js";
import { formatDateTime, getStudentName, resolveErrorMessage } from "./adminUtils.js";

export function AdminClassStudentsPage() {
  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [classStudents, setClassStudents] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState("");
  const [selectedStudentId, setSelectedStudentId] = useState("");
  const [studentSearch, setStudentSearch] = useState({
    studentCode: "",
    fullName: "",
    email: ""
  });
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const selectedClass = useMemo(
    () => classes.find((schoolClass) => schoolClass.classId === selectedClassId),
    [classes, selectedClassId]
  );

  const loadClasses = async () => {
    const data = await adminService.searchClasses({ status: "ACTIVE" });
    setClasses(data ?? []);
    if (!selectedClassId && data?.[0]?.classId) {
      setSelectedClassId(data[0].classId);
    }
  };

  const loadStudents = async (filters = studentSearch) => {
    const data = await adminService.searchStudents(filters);
    setStudents(data ?? []);
  };

  const loadClassStudents = async (classId) => {
    if (!classId) {
      setClassStudents([]);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const data = await adminService.getClassStudents(classId);
      setClassStudents(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải sinh viên trong lớp"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    async function loadInitialData() {
      setError("");
      try {
        await Promise.all([loadClasses(), loadStudents()]);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải dữ liệu ban đầu"));
      }
    }

    loadInitialData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadClassStudents(selectedClassId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedClassId]);

  const updateStudentSearch = (event) => {
    const { name, value } = event.target;
    setStudentSearch((current) => ({ ...current, [name]: value }));
  };

  const handleStudentSearch = async (event) => {
    event.preventDefault();
    setError("");

    try {
      await loadStudents(studentSearch);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tìm kiếm sinh viên"));
    }
  };

  const handleAddStudent = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!selectedClassId || !selectedStudentId) {
      setError("Vui lòng chọn lớp và sinh viên");
      return;
    }

    setSaving(true);
    try {
      await adminService.addStudentToClass(selectedClassId, selectedStudentId);
      setMessage("Đã thêm sinh viên vào lớp");
      setSelectedStudentId("");
      await Promise.all([loadClassStudents(selectedClassId), loadClasses()]);
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
      await adminService.removeStudentFromClass(selectedClassId, student.studentId);
      setMessage("Đã xóa sinh viên khỏi lớp");
      await Promise.all([loadClassStudents(selectedClassId), loadClasses()]);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể xóa sinh viên khỏi lớp"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Quản lý sinh viên trong lớp</h2>
        <p>Chọn lớp, tìm sinh viên và quản lý danh sách sinh viên đang tham gia lớp.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Chọn lớp</h3>
        <div className="form-grid">
          <label>
            Lớp
            <select value={selectedClassId} onChange={(event) => setSelectedClassId(event.target.value)}>
              <option value="">Chọn lớp</option>
              {classes.map((schoolClass) => (
                <option key={schoolClass.classId} value={schoolClass.classId}>
                  {schoolClass.classCode} - {schoolClass.className}
                </option>
              ))}
            </select>
          </label>
          <div className="read-only-box">
            <span>Số sinh viên</span>
            <strong>{selectedClass?.studentCount ?? 0}</strong>
          </div>
        </div>
      </section>

      <div className="admin-two-column">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Danh sách sinh viên trong lớp</h3>
            <span className="muted-text">{classStudents.length} sinh viên</span>
          </div>
          <div className="table-wrapper" aria-busy={loading}>
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
                {classStudents.map((student) => (
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
                {classStudents.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty-cell">
                      Chưa có sinh viên trong lớp này.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <h3>Thêm sinh viên</h3>
          <form className="filter-form compact-filter" onSubmit={handleStudentSearch}>
            <label>
              Mã sinh viên
              <input name="studentCode" value={studentSearch.studentCode} onChange={updateStudentSearch} />
            </label>
            <label>
              Họ tên
              <input name="fullName" value={studentSearch.fullName} onChange={updateStudentSearch} />
            </label>
            <label>
              Email
              <input name="email" value={studentSearch.email} onChange={updateStudentSearch} />
            </label>
            <div className="form-actions">
              <button className="secondary-button" type="submit">
                Tìm sinh viên
              </button>
            </div>
          </form>

          <form className="stacked-form" onSubmit={handleAddStudent}>
            <label>
              Sinh viên
              <select value={selectedStudentId} onChange={(event) => setSelectedStudentId(event.target.value)}>
                <option value="">Chọn sinh viên</option>
                {students.map((student) => (
                  <option key={student.studentId} value={student.studentId}>
                    {student.studentCode} - {getStudentName(student)}
                  </option>
                ))}
              </select>
            </label>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? "Đang thêm..." : "Thêm vào lớp"}
            </button>
          </form>
        </section>
      </div>
    </section>
  );
}
