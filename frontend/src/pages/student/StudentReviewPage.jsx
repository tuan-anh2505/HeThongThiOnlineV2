import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { formatNumber, resolveErrorMessage } from "./studentUtils.js";

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
                {item.answerStatus || "-"}
              </span>
            </div>
            <p className="muted-text">
              {item.type} - {formatNumber(item.score)} điểm
            </p>
            <dl className="profile-details compact-details">
              <div>
                <dt>Câu trả lời của bạn</dt>
                <dd>
                  <pre>{JSON.stringify(item.studentAnswer ?? null, null, 2)}</pre>
                </dd>
              </div>
              {review?.scoreVisible ? (
                <div>
                  <dt>Điểm đạt được</dt>
                  <dd>{formatNumber(item.scoreAchieved)}</dd>
                </div>
              ) : null}
              {item.correctAnswer ? (
                <div>
                  <dt>Đáp án đúng</dt>
                  <dd>
                    <pre>{JSON.stringify(item.correctAnswer, null, 2)}</pre>
                  </dd>
                </div>
              ) : null}
            </dl>
          </article>
        ))}
      </div>
    </section>
  );
}
