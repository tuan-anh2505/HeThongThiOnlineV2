import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, formatNumber, resolveErrorMessage } from "./teacherUtils.js";
import {
  formatAnswerStatus,
  formatCorrectAnswer,
  formatQuestionType,
  formatStudentAnswer,
  shouldShowStudentAnswer
} from "../../utils/reviewAnswerFormatter.js";

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
                  {formatAnswerStatus(item)}
                </span>
              </div>
              <dl className="profile-details compact-details">
                <div>
                  <dt>Loại câu hỏi</dt>
                  <dd>{formatQuestionType(item.question?.type)}</dd>
                </div>
                {shouldShowStudentAnswer(item) ? (
                  <div>
                    <dt>Câu trả lời sinh viên</dt>
                    <dd>
                      <AnswerLines lines={formatStudentAnswer(item)} />
                    </dd>
                  </div>
                ) : null}
                <div>
                  <dt>Đáp án đúng</dt>
                  <dd>
                    <AnswerLines lines={formatCorrectAnswer(item)} />
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

function AnswerLines({ lines }) {
  if (!Array.isArray(lines) || lines.length === 0) {
    return <span>Chưa trả lời</span>;
  }

  if (lines.length === 1) {
    return <span>{lines[0]}</span>;
  }

  return (
    <div className="answer-lines">
      {lines.map((line, index) => (
        <span key={`${line}-${index}`}>{line}</span>
      ))}
    </div>
  );
}
