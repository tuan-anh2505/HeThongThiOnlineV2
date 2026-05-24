import { useEffect, useState } from "react";
import { adminService } from "../../services/adminService.js";
import { formatDateTime, resolveErrorMessage } from "./adminUtils.js";

const SUBJECT_STATUSES = ["ACTIVE", "INACTIVE"];

const initialFilters = {
  subjectCode: "",
  subjectName: "",
  status: ""
};

const initialForm = {
  subjectId: "",
  subjectCode: "",
  subjectName: "",
  description: "",
  status: "ACTIVE"
};

export function AdminSubjectsPage() {
  const [filters, setFilters] = useState(initialFilters);
  const [subjects, setSubjects] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadSubjects = async (nextFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await adminService.searchSubjects(nextFilters);
      setSubjects(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải danh sách môn thi"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSubjects(initialFilters);
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
    loadSubjects(filters);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    loadSubjects(initialFilters);
  };

  const handleEdit = (subject) => {
    setEditing(true);
    setForm({
      subjectId: subject.subjectId,
      subjectCode: subject.subjectCode ?? "",
      subjectName: subject.subjectName ?? "",
      description: subject.description ?? "",
      status: subject.status ?? "ACTIVE"
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!form.subjectCode.trim() || !form.subjectName.trim()) {
      setError("Vui lòng nhập mã môn và tên môn");
      return;
    }

    const payload = {
      subjectCode: form.subjectCode,
      subjectName: form.subjectName,
      description: form.description,
      status: form.status
    };

    setSaving(true);
    try {
      if (editing) {
        await adminService.updateSubject(form.subjectId, payload);
        setMessage("Cập nhật môn thi thành công");
      } else {
        await adminService.createSubject(payload);
        setMessage("Tạo môn thi thành công");
      }

      resetForm();
      await loadSubjects(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu môn thi"));
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (subject) => {
    const isActive = subject.status === "ACTIVE";
    const actionLabel = isActive ? "ngừng hoạt động" : "khôi phục";
    if (!window.confirm(`Bạn có chắc muốn ${actionLabel} dữ liệu này không?`)) {
      return;
    }

    try {
      if (isActive) {
        await adminService.deactivateSubject(subject.subjectId);
        setMessage("Đã ngừng hoạt động môn thi");
      } else {
        await adminService.activateSubject(subject.subjectId);
        setMessage("Đã khôi phục môn thi");
      }
      await loadSubjects(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể cập nhật trạng thái môn thi"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Quản lý môn thi</h2>
        <p>Tạo và cập nhật dữ liệu môn thi dùng chung cho lớp, phân công và bài thi.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Tìm kiếm</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Mã môn
            <input name="subjectCode" value={filters.subjectCode} onChange={updateFilter} />
          </label>
          <label>
            Tên môn
            <input name="subjectName" value={filters.subjectName} onChange={updateFilter} />
          </label>
          <label>
            Trạng thái
            <select name="status" value={filters.status} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {SUBJECT_STATUSES.map((status) => (
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
            <h3>Danh sách môn thi</h3>
            <span className="muted-text">{subjects.length} môn</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã môn</th>
                  <th>Tên môn</th>
                  <th>Mô tả</th>
                  <th>Trạng thái</th>
                  <th>Cập nhật</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {subjects.map((subject) => (
                  <tr key={subject.subjectId}>
                    <td>{subject.subjectCode}</td>
                    <td>{subject.subjectName}</td>
                    <td>{subject.description || "-"}</td>
                    <td>
                      <span className={`status-pill ${subject.status === "ACTIVE" ? "active" : "inactive"}`}>
                        {subject.status}
                      </span>
                    </td>
                    <td>{formatDateTime(subject.updatedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <button className="text-button" type="button" onClick={() => handleEdit(subject)}>
                          Sửa
                        </button>
                        <button
                          className={subject.status === "ACTIVE" ? "danger-text-button" : "text-button"}
                          type="button"
                          onClick={() => handleStatusChange(subject)}
                        >
                          {subject.status === "ACTIVE" ? "Ngừng hoạt động" : "Khôi phục"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {subjects.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty-cell">
                      Chưa có dữ liệu môn thi.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa môn thi" : "Tạo môn thi"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Mã môn
              <input name="subjectCode" value={form.subjectCode} onChange={updateForm} required />
            </label>
            <label>
              Tên môn
              <input name="subjectName" value={form.subjectName} onChange={updateForm} required />
            </label>
            <label>
              Mô tả
              <textarea name="description" rows="4" value={form.description} onChange={updateForm} />
            </label>
            <label>
              Trạng thái
              <select name="status" value={form.status} onChange={updateForm}>
                {SUBJECT_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo môn"}
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
