import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, formatNumber, resolveErrorMessage } from "./teacherUtils.js";

export function TeacherAttemptsPage() {
  const { examId: routeExamId } = useParams();
  const [exams, setExams] = useState([]);
  const [selectedExamId, setSelectedExamId] = useState(routeExamId ?? "");
  const [attempts, setAttempts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    async function loadExams() {
      setError("");
      try {
        const data = await teacherService.searchExams({});
        setExams(data ?? []);
        if (!selectedExamId && data?.[0]?.examId) {
          setSelectedExamId(data[0].examId);
        }
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải danh sách bài thi"));
      }
    }

    loadExams();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    async function loadAttempts() {
      if (!selectedExamId) {
        setAttempts([]);
        return;
      }

      setLoading(true);
      setError("");
      try {
        const data = await teacherService.getExamAttempts(selectedExamId);
        setAttempts(data ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải danh sách bài làm"));
      } finally {
        setLoading(false);
      }
    }

    loadAttempts();
  }, [selectedExamId]);

  const reloadAttempts = async () => {
    if (!selectedExamId) {
      return;
    }
    const data = await teacherService.getExamAttempts(selectedExamId);
    setAttempts(data ?? []);
  };

  const handlePublishResults = async () => {
    if (!window.confirm("Bạn chắc chắn muốn công bố kết quả bài thi này?")) {
      return;
    }

    setError("");
    setMessage("");
    try {
      await teacherService.publishResults(selectedExamId);
      setMessage("Đã công bố kết quả");
      await reloadAttempts();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể công bố kết quả"));
    }
  };

  const handleHideResults = async () => {
    if (!window.confirm("Bạn chắc chắn muốn ẩn kết quả bài thi này?")) {
      return;
    }

    setError("");
    setMessage("");
    try {
      await teacherService.hideResults(selectedExamId);
      setMessage("Đã ẩn kết quả");
      await reloadAttempts();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể ẩn kết quả"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Danh sách bài làm</h2>
        <p>Xem bài làm của sinh viên theo bài thi và công bố hoặc ẩn kết quả.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="form-grid">
          <label>
            Bài thi
            <select value={selectedExamId} onChange={(event) => setSelectedExamId(event.target.value)}>
              <option value="">Chọn bài thi</option>
              {exams.map((exam) => (
                <option key={exam.examId} value={exam.examId}>
                  {exam.examName}
                </option>
              ))}
            </select>
          </label>
          <div className="form-actions">
            <button className="primary-button" type="button" onClick={handlePublishResults} disabled={!selectedExamId}>
              Công bố kết quả
            </button>
            <button className="secondary-button" type="button" onClick={handleHideResults} disabled={!selectedExamId}>
              Ẩn kết quả
            </button>
          </div>
        </div>
      </section>

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Bài làm của sinh viên</h3>
          <span className="muted-text">{loading ? "Đang tải..." : `${attempts.length} bài làm`}</span>
        </div>
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Sinh viên</th>
                <th>Email</th>
                <th>Bắt đầu</th>
                <th>Nộp bài</th>
                <th>Trạng thái</th>
                <th>Điểm</th>
                <th>Lần làm</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {attempts.map((attempt) => (
                <tr key={attempt.attemptId}>
                  <td>
                    {attempt.studentCode} - {attempt.studentName}
                  </td>
                  <td>{attempt.studentEmail || "-"}</td>
                  <td>{formatDateTime(attempt.startedAt)}</td>
                  <td>{formatDateTime(attempt.submittedAt)}</td>
                  <td>
                    <span className={`status-pill ${attempt.status === "SUBMITTED" ? "active" : "inactive"}`}>
                      {attempt.status}
                    </span>
                  </td>
                  <td>
                    {formatNumber(attempt.totalScore)} / {formatNumber(attempt.maxScore)}
                  </td>
                  <td>{attempt.attemptNumber}</td>
                  <td>
                    <Link className="text-link" to={`/teacher/attempts/${attempt.attemptId}`}>
                      Chi tiết
                    </Link>
                  </td>
                </tr>
              ))}
              {attempts.length === 0 ? (
                <tr>
                  <td colSpan="8" className="empty-cell">
                    Chưa có bài làm.
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
