import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, formatNumber, resolveErrorMessage } from "./teacherUtils.js";

export function TeacherAttemptDetailPage() {
  const { attemptId } = useParams();
  const [attempt, setAttempt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadAttempt() {
      setLoading(true);
      setError("");
      try {
        const data = await teacherService.getAttemptDetail(attemptId);
        setAttempt(data);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải chi tiết bài làm"));
      } finally {
        setLoading(false);
      }
    }

    loadAttempt();
  }, [attemptId]);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Chi tiết bài làm</h2>
        <p>Xem câu trả lời, điểm từng câu và đáp án đúng trong bài làm của sinh viên.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>{loading ? "Đang tải..." : attempt?.examName || "Bài làm"}</h3>
          <Link className="text-link" to={attempt?.examId ? `/teacher/exams/${attempt.examId}/attempts` : "/teacher/attempts"}>
            Quay lại danh sách bài làm
          </Link>
        </div>
        {attempt ? (
          <div className="summary-grid compact-summary">
            <div className="metric-card">
              <span>Sinh viên</span>
              <strong>{attempt.studentName}</strong>
            </div>
            <div className="metric-card">
              <span>Trạng thái</span>
              <strong>{attempt.status}</strong>
            </div>
            <div className="metric-card">
              <span>Điểm</span>
              <strong>
                {formatNumber(attempt.totalScore)} / {formatNumber(attempt.maxScore)}
              </strong>
            </div>
            <div className="metric-card">
              <span>Nộp bài</span>
              <strong>{formatDateTime(attempt.submittedAt)}</strong>
            </div>
          </div>
        ) : null}
      </section>

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Danh sách câu trả lời</h3>
          <span className="muted-text">{attempt?.questions?.length ?? 0} câu</span>
        </div>
        <div className="attempt-question-list">
          {(attempt?.questions ?? []).map((item, index) => (
            <article className="attempt-question-card" key={`${item.question?.questionId}-${index}`}>
              <div className="panel-heading compact-heading">
                <h4>
                  Câu {index + 1}: {item.question?.content || "Không có nội dung"}
                </h4>
                <span className={`status-pill ${item.isCorrect ? "active" : "inactive"}`}>
                  {item.answerStatus || "-"}
                </span>
              </div>
              <dl className="profile-details compact-details">
                <div>
                  <dt>Loại câu hỏi</dt>
                  <dd>{item.question?.type || "-"}</dd>
                </div>
                <div>
                  <dt>Câu trả lời sinh viên</dt>
                  <dd>
                    <pre>{JSON.stringify(item.studentAnswer ?? null, null, 2)}</pre>
                  </dd>
                </div>
                <div>
                  <dt>Đáp án đúng</dt>
                  <dd>
                    <pre>{JSON.stringify(item.correctAnswer ?? null, null, 2)}</pre>
                  </dd>
                </div>
                <div>
                  <dt>Điểm đạt được</dt>
                  <dd>{formatNumber(item.scoreAchieved)}</dd>
                </div>
              </dl>
            </article>
          ))}
          {!loading && (attempt?.questions ?? []).length === 0 ? (
            <p className="muted-text">Chưa có dữ liệu câu trả lời.</p>
          ) : null}
        </div>
      </section>
    </section>
  );
}
