import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { formatNumber, resolveErrorMessage } from "./studentUtils.js";
import {
  formatAnswerStatus,
  formatCorrectAnswer,
  formatQuestionType,
  formatStudentAnswer,
  shouldShowStudentAnswer
} from "../../utils/reviewAnswerFormatter.js";

export function StudentReviewPage() {
  const { attemptId } = useParams();
  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadReview() {
      setLoading(true);
      setError("");
      try {
        const data = await studentService.getAttemptReview(attemptId);
        setReview(data);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải review bài làm"));
      } finally {
        setLoading(false);
      }
    }

    loadReview();
  }, [attemptId]);

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Review bài làm</h2>
        <p>Đáp án đúng chỉ hiển thị nếu bài thi cho phép xem đáp án đúng.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>{loading ? "Đang tải..." : review?.examName || "Review"}</h3>
          <Link className="text-link" to={`/student/attempts/${attemptId}/result`}>
            Xem kết quả
          </Link>
        </div>
        {review ? (
          <div className="summary-grid compact-summary">
            <div className="metric-card">
              <span>Môn thi</span>
              <strong>{review.subjectName}</strong>
            </div>
            <div className="metric-card">
              <span>Trạng thái</span>
              <strong>{review.status}</strong>
            </div>
            <div className="metric-card">
              <span>Điểm</span>
              <strong>
                {review.scoreVisible ? `${formatNumber(review.totalScore)} / ${formatNumber(review.maxScore)}` : "Chưa công bố"}
              </strong>
            </div>
            <div className="metric-card">
              <span>Đáp án đúng</span>
              <strong>{review.correctAnswerVisible ? "Được xem" : "Không hiển thị"}</strong>
            </div>
          </div>
        ) : null}
      </section>

      <div className="exam-question-stack">
        {(review?.questions ?? []).map((item, index) => (
          <article
            className={`exam-question-card ${item.isCorrect === true ? "correct-answer" : item.isCorrect === false ? "wrong-answer" : ""}`}
            key={`${item.questionId}-${index}`}
          >
            <div className="panel-heading compact-heading">
              <h3>
                Câu {index + 1}: {item.content}
              </h3>
              <span className={`status-pill ${item.isCorrect ? "active" : "inactive"}`}>
                {formatAnswerStatus(item)}
              </span>
            </div>
            <p className="muted-text">
              {formatQuestionType(item.type)} - {formatNumber(item.score)} điểm
            </p>
            <dl className="profile-details compact-details">
              {shouldShowStudentAnswer(item) ? (
                <div>
                  <dt>Câu trả lời của bạn</dt>
                  <dd>
                    <AnswerLines lines={formatStudentAnswer(item)} />
                  </dd>
                </div>
              ) : null}
              {review?.scoreVisible ? (
                <div>
                  <dt>Điểm đạt được</dt>
                  <dd>{formatNumber(item.scoreAchieved)}</dd>
                </div>
              ) : null}
              {review?.correctAnswerVisible ? (
                <div>
                  <dt>Đáp án đúng</dt>
                  <dd>
                    <AnswerLines lines={formatCorrectAnswer(item)} />
                  </dd>
                </div>
              ) : (
                <div>
                  <dt>Đáp án đúng</dt>
                  <dd className="muted-text">Đáp án đúng chưa được công bố</dd>
                </div>
              )}
            </dl>
          </article>
        ))}
      </div>
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
