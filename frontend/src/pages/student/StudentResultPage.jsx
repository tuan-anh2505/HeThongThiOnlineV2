import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { formatDateTime, formatNumber, resolveErrorMessage } from "./studentUtils.js";

export function StudentResultPage() {
  const { attemptId } = useParams();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadResult() {
      setLoading(true);
      setError("");
      try {
        const data = await studentService.getAttemptResult(attemptId);
        setResult(data);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải kết quả bài thi"));
      } finally {
        setLoading(false);
      }
    }

    loadResult();
  }, [attemptId]);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Kết quả bài làm</h2>
        <p>Điểm chỉ hiển thị nếu bài thi cho phép xem điểm hoặc kết quả đã được công bố.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>{loading ? "Đang tải..." : result?.examName || "Kết quả"}</h3>
          <Link className="text-link" to="/student/results">
            Danh sách kết quả
          </Link>
        </div>

        {result ? (
          <>
            <div className="summary-grid compact-summary">
              <div className="metric-card">
                <span>Môn thi</span>
                <strong>{result.subjectName}</strong>
              </div>
              <div className="metric-card">
                <span>Trạng thái</span>
                <strong>{result.status}</strong>
              </div>
              <div className="metric-card">
                <span>Điểm</span>
                <strong>
                  {result.scoreVisible ? `${formatNumber(result.totalScore)} / ${formatNumber(result.maxScore)}` : "Chưa công bố"}
                </strong>
              </div>
              <div className="metric-card">
                <span>Nộp lúc</span>
                <strong>{formatDateTime(result.submittedAt)}</strong>
              </div>
            </div>

            <div className="form-actions">
              {result.reviewAllowed ? (
                <Link className="primary-link" to={`/student/attempts/${attemptId}/review`}>
                  Review bài làm
                </Link>
              ) : (
                <p className="muted-text">Bài thi chưa cho phép review bài làm.</p>
              )}
            </div>
          </>
        ) : null}
      </section>
    </section>
  );
}
