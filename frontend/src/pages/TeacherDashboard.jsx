import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { teacherService } from "../services/teacherService.js";
import { resolveErrorMessage } from "./teacher/teacherUtils.js";

export function TeacherDashboard() {
  const { user } = useAuth();
  const [summary, setSummary] = useState({
    classes: 0,
    subjects: 0,
    questionBanks: 0,
    exams: 0
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;

    async function loadSummary() {
      setLoading(true);
      setError("");

      const [classes, assignments, questionBanks, exams] = await Promise.allSettled([
        teacherService.getMyClasses(),
        teacherService.getMyAssignments(),
        teacherService.getMyQuestionBanks(),
        teacherService.searchExams({})
      ]);

      if (ignore) {
        return;
      }

      const hasError = [classes, assignments, questionBanks, exams].some((result) => result.status === "rejected");
      if (hasError) {
        setError("Không thể tải đầy đủ dữ liệu tổng quan. Vui lòng kiểm tra backend hoặc quyền truy cập.");
      }

      setSummary({
        classes: classes.status === "fulfilled" ? classes.value?.length ?? 0 : 0,
        subjects: assignments.status === "fulfilled" ? assignments.value?.length ?? 0 : 0,
        questionBanks: questionBanks.status === "fulfilled" ? questionBanks.value?.length ?? 0 : 0,
        exams: exams.status === "fulfilled" ? exams.value?.length ?? 0 : 0
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
        <h2>Trang giảng viên</h2>
        <p>
          Xin chào {user?.fullName || user?.username}. Khu vực này dùng để quản lý lớp phụ trách,
          ngân hàng câu hỏi, bài thi, ca thi, bài làm và thống kê điểm.
        </p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <div className="summary-grid" aria-busy={loading}>
        <Link className="metric-card" to="/teacher/classes">
          <span>Lớp phụ trách</span>
          <strong>{loading ? "-" : summary.classes}</strong>
        </Link>
        <Link className="metric-card" to="/teacher/question-banks">
          <span>Ngân hàng câu hỏi</span>
          <strong>{loading ? "-" : summary.questionBanks}</strong>
        </Link>
        <Link className="metric-card" to="/teacher/exams">
          <span>Bài thi</span>
          <strong>{loading ? "-" : summary.exams}</strong>
        </Link>
        <Link className="metric-card" to="/teacher/statistics">
          <span>Phân công lớp - môn</span>
          <strong>{loading ? "-" : summary.subjects}</strong>
        </Link>
      </div>

      <section className="admin-panel">
        <h3>Thao tác nhanh</h3>
        <div className="quick-actions">
          <Link className="primary-link" to="/teacher/question-banks">
            Tạo ngân hàng câu hỏi
          </Link>
          <Link className="primary-link" to="/teacher/exams">
            Tạo bài thi
          </Link>
          <Link className="primary-link" to="/teacher/statistics">
            Xem thống kê điểm
          </Link>
        </div>
      </section>
    </section>
  );
}
