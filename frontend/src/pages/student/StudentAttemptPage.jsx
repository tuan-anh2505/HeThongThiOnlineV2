import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { studentService } from "../../services/studentService.js";
import { formatDuration, formatNumber, resolveErrorMessage } from "./studentUtils.js";

export function StudentAttemptPage() {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const autoSubmittingRef = useRef(false);
  const [attempt, setAttempt] = useState(null);
  const [answers, setAnswers] = useState({});
  const [remainingSeconds, setRemainingSeconds] = useState(0);
  const [saveStatus, setSaveStatus] = useState({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadAttempt() {
      setLoading(true);
      setError("");
      try {
        const data = await studentService.getAttempt(attemptId);
        setAttempt(data);
        setRemainingSeconds(data.remainingSeconds ?? 0);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải bài làm"));
      } finally {
        setLoading(false);
      }
    }

    loadAttempt();
  }, [attemptId]);

  useEffect(() => {
    if (!attempt || attempt.status !== "IN_PROGRESS") {
      return undefined;
    }

    const timer = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [attempt]);

  useEffect(() => {
    if (!attempt || attempt.status !== "IN_PROGRESS") {
      return undefined;
    }

    const statusTimer = window.setInterval(async () => {
      try {
        const status = await studentService.getAttemptStatus(attemptId);
        setRemainingSeconds(status.remainingSeconds ?? 0);
        if (status.status !== "IN_PROGRESS") {
          navigate(`/student/attempts/${attemptId}/result`, { replace: true });
        }
      } catch {
        // Keep the local timer running. API errors are surfaced by autosave/submit actions.
      }
    }, 20000);

    return () => window.clearInterval(statusTimer);
  }, [attempt, attemptId, navigate]);

  useEffect(() => {
    if (!attempt || attempt.status !== "IN_PROGRESS" || remainingSeconds > 0 || autoSubmittingRef.current) {
      return;
    }

    autoSubmittingRef.current = true;
    studentService
      .autoSubmitAttempt(attemptId)
      .then(() => navigate(`/student/attempts/${attemptId}/result`, { replace: true }))
      .catch((err) => setError(resolveErrorMessage(err, "Không thể tự động nộp bài")));
  }, [attempt, attemptId, navigate, remainingSeconds]);

  const saveAnswer = async (question, nextValue) => {
    if (!attempt || attempt.status !== "IN_PROGRESS") {
      return;
    }

    setSaveStatus((current) => ({ ...current, [question.questionId]: "Đang lưu..." }));
    try {
      await studentService.saveAnswer(attemptId, question.questionId, buildAnswerPayload(question, nextValue));
      setSaveStatus((current) => ({ ...current, [question.questionId]: "Đã lưu" }));
    } catch (err) {
      setSaveStatus((current) => ({
        ...current,
        [question.questionId]: resolveErrorMessage(err, "Lưu câu trả lời thất bại")
      }));
    }
  };

  const updateAnswer = (question, nextValue) => {
    setAnswers((current) => ({ ...current, [question.questionId]: nextValue }));
    saveAnswer(question, nextValue);
  };

  const handleSubmit = async () => {
    if (submitting || !attempt || attempt.status !== "IN_PROGRESS") {
      return;
    }

    if (!window.confirm("Bạn chắc chắn muốn nộp bài?")) {
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      await studentService.submitAttempt(attemptId);
      navigate(`/student/attempts/${attemptId}/result`, { replace: true });
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể nộp bài"));
      setSubmitting(false);
    }
  };

  const inProgress = attempt?.status === "IN_PROGRESS";

  return (
    <section className="exam-taking-page">
      <header className="exam-taking-header">
        <div>
          <p className="eyebrow">Làm bài thi</p>
          <h2>{loading ? "Đang tải..." : attempt?.examName}</h2>
          <p className="muted-text">
            Lần làm {attempt?.attemptNumber ?? "-"} / {attempt?.maxAttempts ?? "-"}
          </p>
        </div>
        <div className="countdown-box">
          <span>Thời gian còn lại</span>
          <strong>{formatDuration(remainingSeconds)}</strong>
        </div>
        <button className="primary-button" type="button" onClick={handleSubmit} disabled={!inProgress || submitting}>
          {submitting ? "Đang nộp..." : "Nộp bài"}
        </button>
      </header>

      {error ? <p className="error-message">{error}</p> : null}

      {!loading && attempt && !inProgress ? (
        <section className="admin-panel">
          <p>Bài làm hiện có trạng thái {attempt.status}. Bạn không thể tiếp tục chỉnh sửa câu trả lời.</p>
          <Link className="primary-link" to={`/student/attempts/${attemptId}/result`}>
            Xem kết quả
          </Link>
        </section>
      ) : null}

      <div className="exam-question-stack">
        {(attempt?.questions ?? []).map((question, index) => (
          <QuestionCard
            key={question.questionId}
            disabled={!inProgress || submitting}
            index={index}
            question={question}
            value={answers[question.questionId]}
            saveStatus={saveStatus[question.questionId]}
            onChange={(nextValue) => updateAnswer(question, nextValue)}
          />
        ))}
      </div>
    </section>
  );
}

function buildAnswerPayload(question, value) {
  if (question.type === "TRUE_FALSE") {
    return {
      questionId: question.questionId,
      trueFalseAnswer: value === true,
      studentAnswer: {
        trueFalseAnswer: value === true
      }
    };
  }

  if (question.type === "MULTIPLE_CHOICE") {
    return {
      questionId: question.questionId,
      selectedOptionId: value,
      studentAnswer: {
        selectedOptionId: value
      }
    };
  }

  if (question.type === "FILL_BLANK") {
    return {
      questionId: question.questionId,
      fillBlankText: value ?? "",
      studentAnswer: {
        fillBlankText: value ?? ""
      }
    };
  }

  const matchingPairs = Object.entries(value ?? {})
    .filter(([, rightText]) => rightText)
    .map(([leftId, rightText]) => ({ leftId, rightText }));

  return {
    questionId: question.questionId,
    matchingPairs,
    studentAnswer: {
      matchingPairs
    }
  };
}

function QuestionCard({ disabled, index, question, value, saveStatus, onChange }) {
  return (
    <article className="exam-question-card">
      <div className="panel-heading compact-heading">
        <h3>
          Câu {index + 1}: {question.content}
        </h3>
        <span className="muted-text">{formatNumber(question.score)} điểm</span>
      </div>
      <p className="muted-text">
        {question.type} {question.difficulty ? `- ${question.difficulty}` : ""} {question.topic ? `- ${question.topic}` : ""}
      </p>

      <QuestionAnswerInput disabled={disabled} question={question} value={value} onChange={onChange} />

      {saveStatus ? <p className="save-status">{saveStatus}</p> : null}
    </article>
  );
}

function QuestionAnswerInput({ disabled, question, value, onChange }) {
  if (question.type === "TRUE_FALSE") {
    return (
      <div className="answer-options">
        <label className="checkbox-label">
          <input type="radio" disabled={disabled} checked={value === true} onChange={() => onChange(true)} />
          Đúng
        </label>
        <label className="checkbox-label">
          <input type="radio" disabled={disabled} checked={value === false} onChange={() => onChange(false)} />
          Sai
        </label>
      </div>
    );
  }

  if (question.type === "MULTIPLE_CHOICE") {
    return (
      <div className="answer-options">
        {(question.options ?? []).map((option) => (
          <label className="checkbox-label answer-option" key={option.optionId}>
            <input
              type="radio"
              disabled={disabled}
              checked={value === option.optionId}
              onChange={() => onChange(option.optionId)}
            />
            {option.content}
          </label>
        ))}
      </div>
    );
  }

  if (question.type === "FILL_BLANK") {
    return (
      <label className="student-answer-input">
        Câu trả lời
        <input disabled={disabled} value={value ?? ""} onChange={(event) => onChange(event.target.value)} />
      </label>
    );
  }

  const currentMatching = value ?? {};
  return (
    <div className="matching-answer-grid">
      {(question.matching?.leftItems ?? []).map((leftItem) => (
        <label key={leftItem.matchingId}>
          {leftItem.leftText}
          <select
            disabled={disabled}
            value={currentMatching[leftItem.matchingId] ?? ""}
            onChange={(event) =>
              onChange({
                ...currentMatching,
                [leftItem.matchingId]: event.target.value
              })
            }
          >
            <option value="">Chọn đáp án</option>
            {(question.matching?.rightItems ?? []).map((rightItem) => (
              <option key={rightItem.rightText} value={rightItem.rightText}>
                {rightItem.rightText}
              </option>
            ))}
          </select>
        </label>
      ))}
    </div>
  );
}
