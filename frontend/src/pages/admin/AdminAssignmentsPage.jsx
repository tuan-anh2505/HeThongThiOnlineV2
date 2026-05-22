import { useEffect, useState } from "react";
import { adminService } from "../../services/adminService.js";
import { formatDateTime, getTeacherName, resolveErrorMessage } from "./adminUtils.js";

const ASSIGNMENT_STATUSES = ["ACTIVE", "INACTIVE"];

const initialFilters = {
  classId: "",
  subjectId: "",
  teacherId: "",
  semester: "",
  schoolYear: "",
  status: ""
};

const initialForm = {
  id: "",
  classId: "",
  subjectId: "",
  teacherId: "",
  semester: "",
  schoolYear: "",
  status: "ACTIVE"
};

export function AdminAssignmentsPage() {
  const [filters, setFilters] = useState(initialFilters);
  const [form, setForm] = useState(initialForm);
  const [assignments, setAssignments] = useState([]);
  const [classes, setClasses] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadAssignments = async (nextFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await adminService.searchAssignments(nextFilters);
      setAssignments(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải danh sách phân công"));
    } finally {
      setLoading(false);
    }
  };

  const loadOptions = async () => {
    const [classList, subjectList, teacherList] = await Promise.all([
      adminService.searchClasses({ status: "ACTIVE" }),
      adminService.searchSubjects({ status: "ACTIVE" }),
      adminService.searchTeachers({})
    ]);
    setClasses(classList ?? []);
    setSubjects(subjectList ?? []);
    setTeachers(teacherList ?? []);
  };

  useEffect(() => {
    async function loadInitialData() {
      try {
        await Promise.all([loadOptions(), loadAssignments(initialFilters)]);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải dữ liệu phân công"));
      }
    }

    loadInitialData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateFilter = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const updateForm = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const resetForm = () => {
    setEditing(false);
    setForm(initialForm);
  };

  const handleSearch = (event) => {
    event.preventDefault();
    loadAssignments(filters);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    loadAssignments(initialFilters);
  };

  const handleEdit = (assignment) => {
    setEditing(true);
    setForm({
      id: assignment.id,
      classId: assignment.classId ?? "",
      subjectId: assignment.subjectId ?? "",
      teacherId: assignment.teacherId ?? "",
      semester: assignment.semester ?? "",
      schoolYear: assignment.schoolYear ?? "",
      status: assignment.status ?? "ACTIVE"
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!form.classId || !form.subjectId || !form.teacherId) {
      setError("Vui lòng chọn lớp, môn thi và giảng viên");
      return;
    }

    const payload = {
      classId: form.classId,
      subjectId: form.subjectId,
      teacherId: form.teacherId,
      semester: form.semester,
      schoolYear: form.schoolYear,
      status: form.status
    };

    setSaving(true);
    try {
      if (editing) {
        await adminService.updateAssignment(form.id, payload);
        setMessage("Cập nhật phân công thành công");
      } else {
        await adminService.createAssignment(payload);
        setMessage("Tạo phân công thành công");
      }

      resetForm();
      await loadAssignments(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu phân công"));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (assignment) => {
    const className = assignment.schoolClass?.classCode || assignment.classId;
    const subjectName = assignment.subject?.subjectCode || assignment.subjectId;
    if (!window.confirm(`Bạn chắc chắn muốn xóa hoặc ngừng hoạt động phân công ${className} - ${subjectName}?`)) {
      return;
    }

    try {
      await adminService.deleteAssignment(assignment.id);
      setMessage("Đã xóa hoặc ngừng hoạt động phân công");
      await loadAssignments(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể xóa phân công"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Phân công lớp - môn - giảng viên</h2>
        <p>Gán giảng viên phụ trách môn thi cho từng lớp học phần.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Tìm kiếm</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Lớp
            <select name="classId" value={filters.classId} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {classes.map((schoolClass) => (
                <option key={schoolClass.classId} value={schoolClass.classId}>
                  {schoolClass.classCode} - {schoolClass.className}
                </option>
              ))}
            </select>
          </label>
          <label>
            Môn thi
            <select name="subjectId" value={filters.subjectId} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {subjects.map((subject) => (
                <option key={subject.subjectId} value={subject.subjectId}>
                  {subject.subjectCode} - {subject.subjectName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Giảng viên
            <select name="teacherId" value={filters.teacherId} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {teachers.map((teacher) => (
                <option key={teacher.teacherId} value={teacher.teacherId}>
                  {teacher.teacherCode} - {getTeacherName(teacher)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Học kỳ
            <input name="semester" value={filters.semester} onChange={updateFilter} />
          </label>
          <label>
            Năm học
            <input name="schoolYear" value={filters.schoolYear} onChange={updateFilter} />
          </label>
          <label>
            Trạng thái
            <select name="status" value={filters.status} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {ASSIGNMENT_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <div className="form-actions">
            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? "Đang tải..." : "Tìm kiếm"}
            </button>
            <button className="secondary-button" type="button" onClick={handleResetFilters}>
              Xóa lọc
            </button>
          </div>
        </form>
      </section>

      <div className="admin-two-column">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Danh sách phân công</h3>
            <span className="muted-text">{assignments.length} phân công</span>
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
                  <th>Trạng thái</th>
                  <th>Cập nhật</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((assignment) => (
                  <tr key={assignment.id}>
                    <td>{assignment.schoolClass?.className || assignment.classId}</td>
                    <td>{assignment.subject?.subjectName || assignment.subjectId}</td>
                    <td>{getTeacherName(assignment.teacher)}</td>
                    <td>{assignment.semester || "-"}</td>
                    <td>{assignment.schoolYear || "-"}</td>
                    <td>
                      <span className={`status-pill ${assignment.status === "ACTIVE" ? "active" : "inactive"}`}>
                        {assignment.status}
                      </span>
                    </td>
                    <td>{formatDateTime(assignment.updatedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <button className="text-button" type="button" onClick={() => handleEdit(assignment)}>
                          Sửa
                        </button>
                        <button className="danger-text-button" type="button" onClick={() => handleDelete(assignment)}>
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {assignments.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="empty-cell">
                      Chưa có dữ liệu phân công.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa phân công" : "Tạo phân công"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Lớp
              <select name="classId" value={form.classId} onChange={updateForm} required>
                <option value="">Chọn lớp</option>
                {classes.map((schoolClass) => (
                  <option key={schoolClass.classId} value={schoolClass.classId}>
                    {schoolClass.classCode} - {schoolClass.className}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Môn thi
              <select name="subjectId" value={form.subjectId} onChange={updateForm} required>
                <option value="">Chọn môn</option>
                {subjects.map((subject) => (
                  <option key={subject.subjectId} value={subject.subjectId}>
                    {subject.subjectCode} - {subject.subjectName}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Giảng viên
              <select name="teacherId" value={form.teacherId} onChange={updateForm} required>
                <option value="">Chọn giảng viên</option>
                {teachers.map((teacher) => (
                  <option key={teacher.teacherId} value={teacher.teacherId}>
                    {teacher.teacherCode} - {getTeacherName(teacher)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Học kỳ
              <input name="semester" value={form.semester} onChange={updateForm} />
            </label>
            <label>
              Năm học
              <input name="schoolYear" value={form.schoolYear} onChange={updateForm} />
            </label>
            <label>
              Trạng thái
              <select name="status" value={form.status} onChange={updateForm}>
                {ASSIGNMENT_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo phân công"}
              </button>
              <button className="secondary-button" type="button" onClick={resetForm}>
                Hủy
              </button>
            </div>
          </form>
        </section>
      </div>
    </section>
  );
}
