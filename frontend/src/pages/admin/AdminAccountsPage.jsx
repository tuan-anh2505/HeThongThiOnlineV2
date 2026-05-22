import { useEffect, useState } from "react";
import { adminService } from "../../services/adminService.js";
import { formatDate, formatDateTime, resolveErrorMessage } from "./adminUtils.js";

const ACCOUNT_STATUSES = ["ACTIVE", "LOCKED"];
const ROLES = ["ADMIN", "TEACHER", "STUDENT"];

const initialFilters = {
  username: "",
  fullName: "",
  email: "",
  phone: "",
  role: "",
  status: ""
};

const initialForm = {
  id: "",
  username: "",
  password: "",
  fullName: "",
  dateOfBirth: "",
  email: "",
  phone: "",
  role: "STUDENT",
  status: "ACTIVE",
  studentCode: "",
  mainClassId: "",
  teacherCode: "",
  adminCode: ""
};

export function AdminAccountsPage() {
  const [filters, setFilters] = useState(initialFilters);
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadAccounts = async (nextFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await adminService.searchAccounts(nextFilters);
      setAccounts(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải danh sách tài khoản"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts(initialFilters);
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
    loadAccounts(filters);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    loadAccounts(initialFilters);
  };

  const handleEdit = (account) => {
    setEditing(true);
    setForm({
      ...initialForm,
      id: account.id,
      username: account.username ?? "",
      fullName: account.fullName ?? "",
      dateOfBirth: account.dateOfBirth ?? "",
      email: account.email ?? "",
      phone: account.phone ?? "",
      role: account.role ?? "STUDENT",
      status: account.status ?? "ACTIVE"
    });
  };

  const buildPayload = () => {
    const payload = {
      username: form.username,
      fullName: form.fullName,
      dateOfBirth: form.dateOfBirth,
      email: form.email,
      phone: form.phone,
      role: form.role,
      status: form.status
    };

    if (!editing || form.password.trim()) {
      payload.password = form.password;
    }

    if (!editing && form.role === "STUDENT") {
      payload.studentProfile = {
        studentCode: form.studentCode,
        mainClassId: form.mainClassId,
        status: "ACTIVE"
      };
    }

    if (!editing && form.role === "TEACHER") {
      payload.teacherProfile = {
        teacherCode: form.teacherCode,
        status: "ACTIVE"
      };
    }

    if (!editing && form.role === "ADMIN") {
      payload.adminProfile = {
        adminCode: form.adminCode
      };
    }

    return payload;
  };

  const validateForm = () => {
    if (!form.username.trim() || !form.fullName.trim() || !form.email.trim()) {
      return "Vui lòng nhập tên đăng nhập, họ tên và email";
    }

    if (!editing && form.password.trim().length < 8) {
      return "Mật khẩu tạo mới phải có ít nhất 8 ký tự";
    }

    if (editing && form.password.trim() && form.password.trim().length < 8) {
      return "Mật khẩu cập nhật phải có ít nhất 8 ký tự";
    }

    if (!editing && form.role === "STUDENT" && !form.studentCode.trim()) {
      return "Vui lòng nhập mã sinh viên";
    }

    if (!editing && form.role === "TEACHER" && !form.teacherCode.trim()) {
      return "Vui lòng nhập mã giảng viên";
    }

    return "";
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    setSaving(true);
    try {
      if (editing) {
        await adminService.updateAccount(form.id, buildPayload());
        setMessage("Cập nhật tài khoản thành công");
      } else {
        await adminService.createAccount(buildPayload());
        setMessage("Tạo tài khoản thành công");
      }

      resetForm();
      await loadAccounts(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu tài khoản"));
    } finally {
      setSaving(false);
    }
  };

  const handleLock = async (account) => {
    if (!window.confirm(`Bạn chắc chắn muốn khóa tài khoản ${account.username}?`)) {
      return;
    }

    try {
      await adminService.lockAccount(account.id);
      setMessage("Đã khóa tài khoản");
      await loadAccounts(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể khóa tài khoản"));
    }
  };

  const handleUnlock = async (account) => {
    if (!window.confirm(`Bạn chắc chắn muốn mở khóa tài khoản ${account.username}?`)) {
      return;
    }

    try {
      await adminService.unlockAccount(account.id);
      setMessage("Đã mở khóa tài khoản");
      await loadAccounts(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể mở khóa tài khoản"));
    }
  };

  const handleDelete = async (account) => {
    if (!window.confirm(`Bạn chắc chắn muốn xóa mềm tài khoản ${account.username}?`)) {
      return;
    }

    try {
      await adminService.deleteAccount(account.id);
      setMessage("Đã xóa mềm tài khoản");
      await loadAccounts(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể xóa tài khoản"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Quản lý tài khoản</h2>
        <p>Tạo, cập nhật, khóa, mở khóa và tìm kiếm tài khoản theo vai trò hoặc trạng thái.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Tìm kiếm</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Tên đăng nhập
            <input name="username" value={filters.username} onChange={updateFilter} />
          </label>
          <label>
            Họ tên
            <input name="fullName" value={filters.fullName} onChange={updateFilter} />
          </label>
          <label>
            Email
            <input name="email" value={filters.email} onChange={updateFilter} />
          </label>
          <label>
            Số điện thoại
            <input name="phone" value={filters.phone} onChange={updateFilter} />
          </label>
          <label>
            Vai trò
            <select name="role" value={filters.role} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
          </label>
          <label>
            Trạng thái
            <select name="status" value={filters.status} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {ACCOUNT_STATUSES.map((status) => (
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
            <h3>Danh sách tài khoản</h3>
            <span className="muted-text">{accounts.length} tài khoản</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Tên đăng nhập</th>
                  <th>Họ tên</th>
                  <th>Email</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>Cập nhật</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((account) => (
                  <tr key={account.id}>
                    <td>{account.username}</td>
                    <td>{account.fullName}</td>
                    <td>{account.email}</td>
                    <td>{account.role}</td>
                    <td>
                      <span className={`status-pill ${account.status === "ACTIVE" ? "active" : "inactive"}`}>
                        {account.status}
                      </span>
                    </td>
                    <td>{formatDateTime(account.updatedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <button className="text-button" type="button" onClick={() => handleEdit(account)}>
                          Sửa
                        </button>
                        {account.status === "LOCKED" ? (
                          <button className="text-button" type="button" onClick={() => handleUnlock(account)}>
                            Mở khóa
                          </button>
                        ) : (
                          <button className="text-button" type="button" onClick={() => handleLock(account)}>
                            Khóa
                          </button>
                        )}
                        <button className="danger-text-button" type="button" onClick={() => handleDelete(account)}>
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {accounts.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-cell">
                      Chưa có dữ liệu tài khoản.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa tài khoản" : "Tạo tài khoản"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Tên đăng nhập
              <input name="username" value={form.username} onChange={updateForm} required />
            </label>
            <label>
              Mật khẩu {editing ? "(để trống nếu không đổi)" : ""}
              <input
                name="password"
                type="password"
                value={form.password}
                onChange={updateForm}
                required={!editing}
                autoComplete="new-password"
              />
            </label>
            <label>
              Họ tên
              <input name="fullName" value={form.fullName} onChange={updateForm} required />
            </label>
            <label>
              Ngày sinh
              <input name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={updateForm} />
            </label>
            <label>
              Email
              <input name="email" type="email" value={form.email} onChange={updateForm} required />
            </label>
            <label>
              Số điện thoại
              <input name="phone" value={form.phone} onChange={updateForm} />
            </label>
            <label>
              Vai trò
              <select name="role" value={form.role} onChange={updateForm}>
                {ROLES.map((role) => (
                  <option key={role} value={role}>
                    {role}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Trạng thái
              <select name="status" value={form.status} onChange={updateForm}>
                {ACCOUNT_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>

            {!editing && form.role === "STUDENT" ? (
              <>
                <label>
                  Mã sinh viên
                  <input name="studentCode" value={form.studentCode} onChange={updateForm} required />
                </label>
                <label>
                  Lớp chính
                  <input name="mainClassId" value={form.mainClassId} onChange={updateForm} />
                </label>
              </>
            ) : null}

            {!editing && form.role === "TEACHER" ? (
              <label>
                Mã giảng viên
                <input name="teacherCode" value={form.teacherCode} onChange={updateForm} required />
              </label>
            ) : null}

            {!editing && form.role === "ADMIN" ? (
              <label>
                Mã admin
                <input name="adminCode" value={form.adminCode} onChange={updateForm} />
              </label>
            ) : null}

            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo tài khoản"}
              </button>
              <button className="secondary-button" type="button" onClick={resetForm}>
                Hủy
              </button>
            </div>
          </form>

          {editing ? <p className="muted-text">Ngày sinh hiện tại: {formatDate(form.dateOfBirth)}</p> : null}
        </section>
      </div>
    </section>
  );
}
