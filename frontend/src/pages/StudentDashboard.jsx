import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { studentService } from "../services/studentService.js";
import { resolveErrorMessage } from "./student/studentUtils.js";

export function StudentDashboard() {
  const { user } = useAuth();
  const [summary, setSummary] = useState({
    subjects: 0,
    exams: 0,
    results: 0
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;

    async function loadSummary() {
      setLoading(true);
      setError("");
      const [subjects, exams, results] = await Promise.allSettled([
        studentService.getSubjects(),
        studentService.getExams(),
        studentService.getResults()
      ]);

      if (ignore) {
        return;
      }

      if ([subjects, exams, results].some((result) => result.status === "rejected")) {
        setError("Không thể tải đầy đủ dữ liệu tổng quan. Vui lòng kiểm tra lại kết nối backend.");
      }

      setSummary({
        subjects: subjects.status === "fulfilled" ? subjects.value?.length ?? 0 : 0,
        exams: exams.status === "fulfilled" ? exams.value?.length ?? 0 : 0,
        results: results.status === "fulfilled" ? results.value?.length ?? 0 : 0
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
        <h2>Trang sinh viên</h2>
        <p>
          Xin chào {user?.fullName || user?.username}. Khu vực này dùng để xem môn thi, bài thi được
          giao, làm bài và xem kết quả khi được công bố.
        </p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <div className="summary-grid" aria-busy={loading}>
        <Link className="metric-card" to="/student/subjects">
          <span>Môn thi</span>
          <strong>{loading ? "-" : summary.subjects}</strong>
        </Link>
        <Link className="metric-card" to="/student/exams">
          <span>Bài thi được giao</span>
          <strong>{loading ? "-" : summary.exams}</strong>
        </Link>
        <Link className="metric-card" to="/student/results">
          <span>Kết quả</span>
          <strong>{loading ? "-" : summary.results}</strong>
        </Link>
      </div>
    </section>
  );
}
