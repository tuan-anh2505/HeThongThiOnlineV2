import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { formatDateTime, resolveErrorMessage } from "./studentUtils.js";

export function StudentExamDetailPage() {
  const { examId } = useParams();
  const navigate = useNavigate();
  const [exam, setExam] = useState(null);
  const [examPassword, setExamPassword] = useState("");
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadExam() {
      setLoading(true);
      setError("");
      try {
        const data = await studentService.getExamDetail(examId);
        setExam(data);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải chi tiết bài thi"));
      } finally {
        setLoading(false);
      }
    }

    loadExam();
  }, [examId]);

  const handleStart = async (event) => {
    event.preventDefault();
    setError("");

    if (exam?.hasPassword && !examPassword.trim()) {
      setError("Bài thi yêu cầu mật khẩu");
      return;
    }

    setStarting(true);
    try {
      const attempt = await studentService.startExam(examId, examPassword);
      navigate(`/student/attempts/${attempt.attemptId}`, { replace: true });
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể bắt đầu bài thi"));
    } finally {
      setStarting(false);
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Chi tiết bài thi</h2>
        <p>Xem thông tin bài thi trước khi bắt đầu làm bài.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>{loading ? "Đang tải..." : exam?.examName || "Bài thi"}</h3>
          <Link className="text-link" to="/student/exams">
            Quay lại danh sách bài thi
          </Link>
        </div>

        {exam ? (
          <>
            <div className="summary-grid compact-summary">
              <div className="metric-card">
                <span>Môn thi</span>
                <strong>{exam.subjectName}</strong>
              </div>
              <div className="metric-card">
                <span>Lớp</span>
                <strong>{exam.className}</strong>
              </div>
              <div className="metric-card">
                <span>Thời gian</span>
                <strong>{exam.durationMinutes} phút</strong>
              </div>
              <div className="metric-card">
                <span>Lần làm</span>
                <strong>
                  {exam.attemptCount} / {exam.maxAttempts}
                </strong>
              </div>
            </div>

            <dl className="profile-details">
              <div>
                <dt>Ca thi</dt>
                <dd>
                  {formatDateTime(exam.startTime)} - {formatDateTime(exam.endTime)}
                </dd>
              </div>
              <div>
                <dt>Trạng thái bài thi</dt>
                <dd>{exam.status}</dd>
              </div>
              <div>
                <dt>Trạng thái ca thi</dt>
                <dd>{exam.sessionStatus || "-"}</dd>
              </div>
              <div>
                <dt>Cấu hình</dt>
                <dd>
                  Xem điểm: {exam.allowViewScore ? "Có" : "Không"}; Review: {exam.allowReview ? "Có" : "Không"};
                  Xem đáp án đúng: {exam.allowViewCorrectAnswer ? "Có" : "Không"}
                </dd>
              </div>
            </dl>

            <form className="stacked-form start-exam-form" onSubmit={handleStart}>
              {exam.hasPassword ? (
                <label>
                  Mật khẩu bài thi
                  <input
                    type="password"
                    value={examPassword}
                    onChange={(event) => setExamPassword(event.target.value)}
                    autoComplete="current-password"
                  />
                </label>
              ) : null}
              <button className="primary-button" type="submit" disabled={starting}>
                {starting ? "Đang bắt đầu..." : "Bắt đầu làm bài"}
              </button>
            </form>
          </>
        ) : null}
      </section>
    </section>
  );
}
