import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatNumber, resolveErrorMessage } from "./teacherUtils.js";

const QUESTION_TYPES = ["TRUE_FALSE", "MULTIPLE_CHOICE", "FILL_BLANK", "MATCHING"];
const DIFFICULTIES = ["EASY", "MEDIUM", "HARD"];

function createRandomConfig() {
  return {
    type: "MULTIPLE_CHOICE",
    difficulty: "EASY",
    topic: "",
    quantity: "5"
  };
}

export function TeacherExamQuestionsPage() {
  const { examId } = useParams();
  const [exam, setExam] = useState(null);
  const [bankQuestions, setBankQuestions] = useState([]);
  const [selectedQuestionId, setSelectedQuestionId] = useState("");
  const [selectedScore, setSelectedScore] = useState("");
  const [randomConfigs, setRandomConfigs] = useState([createRandomConfig()]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const examQuestionIds = useMemo(
    () => new Set((exam?.questions ?? []).map((item) => item.questionId)),
    [exam]
  );

  const availableQuestions = useMemo(
    () => bankQuestions.filter((question) => !examQuestionIds.has(question.questionId)),
    [bankQuestions, examQuestionIds]
  );

  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      const examData = await teacherService.getExam(examId);
      setExam(examData);
      if (examData.questionBankId) {
        const questions = await teacherService.searchQuestions(examData.questionBankId, { status: "ACTIVE" });
        setBankQuestions(questions ?? []);
      } else {
        setBankQuestions([]);
      }
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải dữ liệu câu hỏi bài thi"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [examId]);

  const handleAddQuestion = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!selectedQuestionId) {
      setError("Vui lòng chọn câu hỏi");
      return;
    }

    const question = bankQuestions.find((item) => item.questionId === selectedQuestionId);
    setSaving(true);
    try {
      await teacherService.addQuestionToExam(examId, {
        questionId: selectedQuestionId,
        score: selectedScore ? Number(selectedScore) : Number(question?.score ?? 1),
        orderIndex: (exam?.questions?.length ?? 0) + 1
      });
      setMessage("Đã thêm câu hỏi vào bài thi");
      setSelectedQuestionId("");
      setSelectedScore("");
      await loadData();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể thêm câu hỏi vào bài thi"));
    } finally {
      setSaving(false);
    }
  };

  const handleRemoveQuestion = async (questionId) => {
    if (!window.confirm("Bạn chắc chắn muốn xóa câu hỏi khỏi bài thi?")) {
      return;
    }

    setError("");
    setMessage("");
    try {
      await teacherService.removeQuestionFromExam(examId, questionId);
      setMessage("Đã xóa câu hỏi khỏi bài thi");
      await loadData();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể xóa câu hỏi khỏi bài thi"));
    }
  };

  const updateRandomConfig = (index, field, value) => {
    setRandomConfigs((current) =>
      current.map((config, configIndex) => (configIndex === index ? { ...config, [field]: value } : config))
    );
  };

  const addRandomConfig = () => {
    setRandomConfigs((current) => [...current, createRandomConfig()]);
  };

  const removeRandomConfig = (index) => {
    setRandomConfigs((current) => current.filter((_, configIndex) => configIndex !== index));
  };

  const handleGenerateRandom = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    const configs = randomConfigs.map((config) => ({
      type: config.type,
      difficulty: config.difficulty,
      topic: config.topic,
      quantity: Number(config.quantity)
    }));

    if (configs.some((config) => !config.quantity || config.quantity <= 0)) {
      setError("Số lượng random phải lớn hơn 0");
      return;
    }

    setSaving(true);
    try {
      await teacherService.generateRandomQuestions(examId, configs);
      setMessage("Đã random câu hỏi cho bài thi");
      await loadData();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể random câu hỏi"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Danh sách câu hỏi trong bài thi</h2>
        <p>Chọn câu hỏi thủ công hoặc random câu hỏi từ ngân hàng câu hỏi của bài thi.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>{exam?.examName || "Bài thi"}</h3>
          <Link className="text-link" to="/teacher/exams">
            Quay lại danh sách bài thi
          </Link>
        </div>
        <div className="summary-grid compact-summary">
          <div className="metric-card">
            <span>Trạng thái</span>
            <strong>{exam?.status || "-"}</strong>
          </div>
          <div className="metric-card">
            <span>Số câu hỏi</span>
            <strong>{loading ? "-" : exam?.questions?.length ?? 0}</strong>
          </div>
          <div className="metric-card">
            <span>Tổng điểm</span>
            <strong>{formatNumber(exam?.totalScore)}</strong>
          </div>
          <div className="metric-card">
            <span>Ngân hàng</span>
            <strong>{exam?.questionBank?.name || "-"}</strong>
          </div>
        </div>
      </section>

      <div className="admin-two-column">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Câu hỏi đã chọn</h3>
            <span className="muted-text">{exam?.questions?.length ?? 0} câu hỏi</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Thứ tự</th>
                  <th>Nội dung</th>
                  <th>Loại</th>
                  <th>Điểm</th>
                  <th>Mức độ</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {(exam?.questions ?? []).map((item) => (
                  <tr key={item.questionId}>
                    <td>{item.orderIndex}</td>
                    <td>{item.question?.content || item.questionId}</td>
                    <td>{item.question?.type || "-"}</td>
                    <td>{formatNumber(item.score)}</td>
                    <td>{item.question?.difficulty || "-"}</td>
                    <td>
                      <button
                        className="danger-text-button"
                        type="button"
                        onClick={() => handleRemoveQuestion(item.questionId)}
                      >
                        Xóa khỏi đề
                      </button>
                    </td>
                  </tr>
                ))}
                {(exam?.questions ?? []).length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty-cell">
                      Bài thi chưa có câu hỏi.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <h3>Chọn thủ công</h3>
          <form className="stacked-form" onSubmit={handleAddQuestion}>
            <label>
              Câu hỏi trong ngân hàng
              <select value={selectedQuestionId} onChange={(event) => setSelectedQuestionId(event.target.value)}>
                <option value="">Chọn câu hỏi</option>
                {availableQuestions.map((question) => (
                  <option key={question.questionId} value={question.questionId}>
                    {question.type} - {question.content}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Điểm
              <input
                type="number"
                min="0.1"
                step="0.1"
                value={selectedScore}
                onChange={(event) => setSelectedScore(event.target.value)}
                placeholder="Để trống để dùng điểm câu hỏi"
              />
            </label>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? "Đang thêm..." : "Thêm câu hỏi"}
            </button>
          </form>
        </section>
      </div>

      <section className="admin-panel">
        <div className="panel-heading">
          <h3>Random câu hỏi</h3>
          <button className="text-button" type="button" onClick={addRandomConfig}>
            Thêm cấu hình
          </button>
        </div>
        <form className="stacked-form" onSubmit={handleGenerateRandom}>
          {randomConfigs.map((config, index) => (
            <div className="random-config-row" key={`${config.type}-${index}`}>
              <label>
                Loại
                <select value={config.type} onChange={(event) => updateRandomConfig(index, "type", event.target.value)}>
                  {QUESTION_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Mức độ
                <select
                  value={config.difficulty}
                  onChange={(event) => updateRandomConfig(index, "difficulty", event.target.value)}
                >
                  {DIFFICULTIES.map((difficulty) => (
                    <option key={difficulty} value={difficulty}>
                      {difficulty}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Chủ đề
                <input value={config.topic} onChange={(event) => updateRandomConfig(index, "topic", event.target.value)} />
              </label>
              <label>
                Số lượng
                <input
                  type="number"
                  min="1"
                  value={config.quantity}
                  onChange={(event) => updateRandomConfig(index, "quantity", event.target.value)}
                />
              </label>
              <button className="danger-text-button" type="button" onClick={() => removeRandomConfig(index)}>
                Xóa
              </button>
            </div>
          ))}
          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? "Đang random..." : "Random câu hỏi"}
          </button>
        </form>
      </section>
    </section>
  );
}
