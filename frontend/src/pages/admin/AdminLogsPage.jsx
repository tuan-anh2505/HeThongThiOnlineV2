import { useEffect, useState } from "react";
import { adminService } from "../../services/adminService.js";
import { formatDateTime, resolveErrorMessage } from "./adminUtils.js";

const initialFilters = {
  accountId: "",
  action: "",
  targetType: "",
  targetId: "",
  ipAddress: "",
  from: "",
  to: ""
};

function toIsoDateTime(value) {
  if (!value) {
    return "";
  }

  return new Date(value).toISOString();
}

export function AdminLogsPage() {
  const [filters, setFilters] = useState(initialFilters);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const buildFilters = (currentFilters) => ({
    ...currentFilters,
    from: toIsoDateTime(currentFilters.from),
    to: toIsoDateTime(currentFilters.to)
  });

  const loadLogs = async (nextFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await adminService.searchLogs(buildFilters(nextFilters));
      setLogs(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải nhật ký hệ thống"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs(initialFilters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateFilter = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const handleSearch = (event) => {
    event.preventDefault();
    loadLogs(filters);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    loadLogs(initialFilters);
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Nhật ký hệ thống</h2>
        <p>Theo dõi các hành động quan trọng trong hệ thống theo tài khoản, hành động và thời gian.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Tìm kiếm</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Account ID
            <input name="accountId" value={filters.accountId} onChange={updateFilter} />
          </label>
          <label>
            Hành động
            <input name="action" value={filters.action} onChange={updateFilter} />
          </label>
          <label>
            Loại đối tượng
            <input name="targetType" value={filters.targetType} onChange={updateFilter} />
          </label>
          <label>
            Target ID
            <input name="targetId" value={filters.targetId} onChange={updateFilter} />
          </label>
          <label>
            IP
            <input name="ipAddress" value={filters.ipAddress} onChange={updateFilter} />
          </label>
          <label>
            Từ thời điểm
            <input name="from" type="datetime-local" value={filters.from} onChange={updateFilter} />
          </label>
          <label>
            Đến thời điểm
            <input name="to" type="datetime-local" value={filters.to} onChange={updateFilter} />
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

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Danh sách nhật ký</h3>
          <span className="muted-text">{logs.length} bản ghi</span>
        </div>
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Account ID</th>
                <th>Hành động</th>
                <th>Loại đối tượng</th>
                <th>Target ID</th>
                <th>IP</th>
                <th>Mô tả</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.logId}>
                  <td>{formatDateTime(log.createdAt)}</td>
                  <td>{log.accountId || "-"}</td>
                  <td>{log.action}</td>
                  <td>{log.targetType}</td>
                  <td>{log.targetId || "-"}</td>
                  <td>{log.ipAddress || "-"}</td>
                  <td>{log.description || "-"}</td>
                </tr>
              ))}
              {logs.length === 0 ? (
                <tr>
                  <td colSpan="7" className="empty-cell">
                    Chưa có dữ liệu nhật ký.
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
