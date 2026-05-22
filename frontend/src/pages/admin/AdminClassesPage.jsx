import { useEffect, useMemo, useState } from "react";
import { adminService } from "../../services/adminService.js";
import { formatDateTime, getTeacherName, resolveErrorMessage } from "./adminUtils.js";

const CLASS_STATUSES = ["ACTIVE", "INACTIVE"];

const initialFilters = {
  classCode: "",
  className: "",
  teacherId: "",
  status: ""
};

const initialForm = {
  classId: "",
  classCode: "",
  className: "",
  teacherId: "",
  status: "ACTIVE"
};

export function AdminClassesPage() {
  const [filters, setFilters] = useState(initialFilters);
  const [classes, setClasses] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const teachersById = useMemo(
    () => new Map(teachers.map((teacher) => [teacher.teacherId, teacher])),
    [teachers]
  );

  const loadClasses = async (nextFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await adminService.searchClasses(nextFilters);
      setClasses(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải danh sách lớp"));
    } finally {
      setLoading(false);
    }
  };

  const loadTeachers = async () => {
    try {
      const data = await adminService.searchTeachers({});
      setTeachers(data ?? []);
    } catch {
      setTeachers([]);
    }
  };

  useEffect(() => {
    loadClasses(initialFilters);
    loadTeachers();
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
    loadClasses(filters);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    loadClasses(initialFilters);
  };

  const handleEdit = (schoolClass) => {
    setEditing(true);
    setForm({
      classId: schoolClass.classId,
      classCode: schoolClass.classCode ?? "",
      className: schoolClass.className ?? "",
      teacherId: schoolClass.teacherId ?? "",
      status: schoolClass.status ?? "ACTIVE"
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!form.classCode.trim() || !form.className.trim()) {
      setError("Vui lòng nhập mã lớp và tên lớp");
      return;
    }

    const payload = {
      classCode: form.classCode,
      className: form.className,
      teacherId: form.teacherId,
      status: form.status
    };

    setSaving(true);
    try {
      if (editing) {
        await adminService.updateClass(form.classId, payload);
        setMessage("Cập nhật lớp thành công");
      } else {
        await adminService.createClass(payload);
        setMessage("Tạo lớp thành công");
      }

      resetForm();
      await loadClasses(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu lớp"));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (schoolClass) => {
    if (!window.confirm(`Bạn chắc chắn muốn xóa hoặc ngừng hoạt động lớp ${schoolClass.classCode}?`)) {
      return;
    }

    try {
      await adminService.deleteClass(schoolClass.classId);
      setMessage("Đã xóa hoặc ngừng hoạt động lớp");
      await loadClasses(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể xóa lớp"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Quản lý lớp</h2>
        <p>Tạo, cập nhật, tìm kiếm và ngừng hoạt động lớp học phần.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Tìm kiếm</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Mã lớp
            <input name="classCode" value={filters.classCode} onChange={updateFilter} />
          </label>
          <label>
            Tên lớp
            <input name="className" value={filters.className} onChange={updateFilter} />
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
            Trạng thái
            <select name="status" value={filters.status} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {CLASS_STATUSES.map((status) => (
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
            <h3>Danh sách lớp</h3>
            <span className="muted-text">{classes.length} lớp</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã lớp</th>
                  <th>Tên lớp</th>
                  <th>Giảng viên</th>
                  <th>Sinh viên</th>
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
                    <td>{getTeacherName(teachersById.get(schoolClass.teacherId))}</td>
                    <td>{schoolClass.studentCount}</td>
                    <td>
                      <span className={`status-pill ${schoolClass.status === "ACTIVE" ? "active" : "inactive"}`}>
                        {schoolClass.status}
                      </span>
                    </td>
                    <td>{formatDateTime(schoolClass.updatedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <button className="text-button" type="button" onClick={() => handleEdit(schoolClass)}>
                          Sửa
                        </button>
                        <button className="danger-text-button" type="button" onClick={() => handleDelete(schoolClass)}>
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {classes.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-cell">
                      Chưa có dữ liệu lớp.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa lớp" : "Tạo lớp"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Mã lớp
              <input name="classCode" value={form.classCode} onChange={updateForm} required />
            </label>
            <label>
              Tên lớp
              <input name="className" value={form.className} onChange={updateForm} required />
            </label>
            <label>
              Giảng viên phụ trách
              <select name="teacherId" value={form.teacherId} onChange={updateForm}>
                <option value="">Chưa gán</option>
                {teachers.map((teacher) => (
                  <option key={teacher.teacherId} value={teacher.teacherId}>
                    {teacher.teacherCode} - {getTeacherName(teacher)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Trạng thái
              <select name="status" value={form.status} onChange={updateForm}>
                {CLASS_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo lớp"}
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
