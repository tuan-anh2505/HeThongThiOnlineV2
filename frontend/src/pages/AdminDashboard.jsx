import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { adminService } from "../services/adminService.js";
import { formatDateTime, resolveErrorMessage } from "./admin/adminUtils.js";

export function AdminDashboard() {
  const { user } = useAuth();
  const [summary, setSummary] = useState({
    accounts: 0,
    classes: 0,
    subjects: 0,
    questionBanks: 0,
    recentLogs: []
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;

    async function loadSummary() {
      setLoading(true);
      setError("");

      const [accounts, classes, subjects, questionBanks, logs] = await Promise.allSettled([
        adminService.searchAccounts({}),
        adminService.searchClasses({}),
        adminService.searchSubjects({}),
        adminService.searchQuestionBanks({}),
        adminService.searchLogs({})
      ]);

      if (ignore) {
        return;
      }

      const hasError = [accounts, classes, subjects, questionBanks, logs].some(
        (result) => result.status === "rejected"
      );

      if (hasError) {
        setError("Không thể tải đầy đủ dữ liệu tổng quan. Vui lòng kiểm tra backend hoặc quyền truy cập.");
      }

      const logItems = logs.status === "fulfilled" ? logs.value ?? [] : [];
      setSummary({
        accounts: accounts.status === "fulfilled" ? accounts.value?.length ?? 0 : 0,
        classes: classes.status === "fulfilled" ? classes.value?.length ?? 0 : 0,
        subjects: subjects.status === "fulfilled" ? subjects.value?.length ?? 0 : 0,
        questionBanks: questionBanks.status === "fulfilled" ? questionBanks.value?.length ?? 0 : 0,
        recentLogs: logItems.slice(0, 5)
      });
      setLoading(false);
    }

    loadSummary().catch((err) => {
      if (!ignore) {
        setError(resolveErrorMessage(err));
        setLoading(false);
      }
    });

    return () => {
      ignore = true;
    };
  }, []);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Trang quản trị</h2>
        <p>
          Xin chào {user?.fullName || user?.username}. Khu vực này dùng để quản lý tài khoản, lớp,
          môn thi, ngân hàng câu hỏi, thống kê và nhật ký hệ thống.
        </p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <div className="summary-grid" aria-busy={loading}>
        <Link className="metric-card" to="/admin/accounts">
          <span>Tài khoản</span>
          <strong>{loading ? "-" : summary.accounts}</strong>
        </Link>
        <Link className="metric-card" to="/admin/classes">
          <span>Lớp</span>
          <strong>{loading ? "-" : summary.classes}</strong>
        </Link>
        <Link className="metric-card" to="/admin/subjects">
          <span>Môn thi</span>
          <strong>{loading ? "-" : summary.subjects}</strong>
        </Link>
        <Link className="metric-card" to="/admin/question-banks">
          <span>Ngân hàng câu hỏi</span>
          <strong>{loading ? "-" : summary.questionBanks}</strong>
        </Link>
      </div>

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Nhật ký gần đây</h3>
          <Link className="text-link" to="/admin/logs">
            Xem tất cả
          </Link>
        </div>

        {summary.recentLogs.length > 0 ? (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Hành động</th>
                  <th>Đối tượng</th>
                  <th>Mô tả</th>
                </tr>
              </thead>
              <tbody>
                {summary.recentLogs.map((log) => (
                  <tr key={log.logId}>
                    <td>{formatDateTime(log.createdAt)}</td>
                    <td>{log.action}</td>
                    <td>{log.targetType}</td>
                    <td>{log.description || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="muted-text">Chưa có dữ liệu nhật ký để hiển thị.</p>
        )}
      </section>
    </section>
  );
}
