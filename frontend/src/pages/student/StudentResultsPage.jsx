import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { formatDateTime, formatNumber, resolveErrorMessage } from "./studentUtils.js";

export function StudentResultsPage() {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadResults() {
      setLoading(true);
      setError("");
      try {
        const data = await studentService.getResults();
        setResults(data ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải kết quả bài thi"));
      } finally {
        setLoading(false);
      }
    }

    loadResults();
  }, []);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Kết quả bài thi</h2>
        <p>Danh sách kết quả các lần làm bài của sinh viên.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Kết quả của tôi</h3>
          <span className="muted-text">{loading ? "Đang tải..." : `${results.length} kết quả`}</span>
        </div>
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Bài thi</th>
                <th>Môn</th>
                <th>Lần làm</th>
                <th>Trạng thái</th>
                <th>Nộp lúc</th>
                <th>Điểm</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {results.map((result) => (
                <tr key={result.attemptId}>
                  <td>{result.examName}</td>
                  <td>{result.subjectName}</td>
                  <td>{result.attemptNumber}</td>
                  <td>{result.status}</td>
                  <td>{formatDateTime(result.submittedAt)}</td>
                  <td>
                    {result.scoreVisible ? `${formatNumber(result.totalScore)} / ${formatNumber(result.maxScore)}` : "Chưa công bố"}
                  </td>
                  <td>
                    <div className="row-actions">
                      <Link className="text-link" to={`/student/attempts/${result.attemptId}/result`}>
                        Xem kết quả
                      </Link>
                      {result.reviewAllowed ? (
                        <Link className="text-link" to={`/student/attempts/${result.attemptId}/review`}>
                          Review
                        </Link>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
              {results.length === 0 ? (
                <tr>
                  <td colSpan="7" className="empty-cell">
                    Chưa có kết quả bài thi.
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
