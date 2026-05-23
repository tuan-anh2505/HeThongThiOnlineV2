import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, resolveErrorMessage } from "./teacherUtils.js";

const QUESTION_TYPES = ["TRUE_FALSE", "MULTIPLE_CHOICE", "FILL_BLANK", "MATCHING"];
const DIFFICULTIES = ["EASY", "MEDIUM", "HARD"];
const QUESTION_STATUSES = ["ACTIVE", "INACTIVE"];

const initialFilters = {
  type: "",
  content: "",
  difficulty: "",
  topic: "",
  status: ""
};

function createDefaultOptions() {
  return ["A", "B", "C", "D"].map((optionId, index) => ({
    optionId,
    content: "",
    isCorrect: optionId === "A",
    orderIndex: index + 1
  }));
}

function createInitialForm() {
  return {
    questionId: "",
    type: "MULTIPLE_CHOICE",
    content: "",
    score: "1",
    difficulty: "EASY",
    topic: "",
    status: "ACTIVE",
    trueFalseAnswer: "true",
    options: createDefaultOptions(),
    fillBlank: {
      answerId: "",
      acceptedAnswers: "",
      ignoreCase: true,
      ignoreAccent: false,
      trimSpace: true
    },
    matchingPairs: [
      { matchingId: "M1", leftText: "", rightText: "", orderIndex: 1 },
      { matchingId: "M2", leftText: "", rightText: "", orderIndex: 2 }
    ]
  };
}

export function TeacherQuestionsPage() {
  const { bankId: routeBankId } = useParams();
  const [questionBanks, setQuestionBanks] = useState([]);
  const [selectedBankId, setSelectedBankId] = useState(routeBankId ?? "");
  const [filters, setFilters] = useState(initialFilters);
  const [questions, setQuestions] = useState([]);
  const [form, setForm] = useState(createInitialForm);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    async function loadBanks() {
      setError("");
      try {
        const data = await teacherService.getMyQuestionBanks();
        setQuestionBanks(data ?? []);
        if (!selectedBankId && data?.[0]?.questionBankId) {
          setSelectedBankId(data[0].questionBankId);
        }
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải ngân hàng câu hỏi"));
      }
    }

    loadBanks();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (selectedBankId) {
      loadQuestions(selectedBankId, filters);
    } else {
      setQuestions([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedBankId]);

  const loadQuestions = async (bankId = selectedBankId, nextFilters = filters) => {
    if (!bankId) {
      return;
    }

    setLoading(true);
    setError("");
    try {
      const data = await teacherService.searchQuestions(bankId, nextFilters);
      setQuestions(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải danh sách câu hỏi"));
    } finally {
      setLoading(false);
    }
  };

  const updateFilter = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const updateForm = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  };

  const updateFillBlank = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((current) => ({
      ...current,
      fillBlank: {
        ...current.fillBlank,
        [name]: type === "checkbox" ? checked : value
      }
    }));
  };

  const updateOption = (index, field, value) => {
    setForm((current) => ({
      ...current,
      options: current.options.map((option, optionIndex) =>
        optionIndex === index ? { ...option, [field]: value } : option
      )
    }));
  };

  const setCorrectOption = (optionId) => {
    setForm((current) => ({
      ...current,
      options: current.options.map((option) => ({ ...option, isCorrect: option.optionId === optionId }))
    }));
  };

  const updateMatchingPair = (index, field, value) => {
    setForm((current) => ({
      ...current,
      matchingPairs: current.matchingPairs.map((pair, pairIndex) =>
        pairIndex === index ? { ...pair, [field]: value } : pair
      )
    }));
  };

  const addMatchingPair = () => {
    setForm((current) => ({
      ...current,
      matchingPairs: [
        ...current.matchingPairs,
        {
          matchingId: `M${current.matchingPairs.length + 1}`,
          leftText: "",
          rightText: "",
          orderIndex: current.matchingPairs.length + 1
        }
      ]
    }));
  };

  const removeMatchingPair = (index) => {
    setForm((current) => ({
      ...current,
      matchingPairs: current.matchingPairs.filter((_, pairIndex) => pairIndex !== index)
    }));
  };

  const resetForm = () => {
    setEditing(false);
    setForm(createInitialForm());
  };

  const handleSearch = (event) => {
    event.preventDefault();
    loadQuestions(selectedBankId, filters);
  };

  const buildAnswerPayload = () => {
    if (form.type === "TRUE_FALSE") {
      return {
        correctAnswer: form.trueFalseAnswer === "true"
      };
    }

    if (form.type === "MULTIPLE_CHOICE") {
      return {
        options: form.options
          .filter((option) => option.content.trim())
          .map((option, index) => ({
            optionId: option.optionId || `O${index + 1}`,
            content: option.content,
            isCorrect: option.isCorrect,
            orderIndex: Number(option.orderIndex || index + 1)
          }))
      };
    }

    if (form.type === "FILL_BLANK") {
      return {
        fillBlank: {
          answerId: form.fillBlank.answerId || "F1",
          acceptedAnswers: form.fillBlank.acceptedAnswers
            .split("|")
            .map((answer) => answer.trim())
            .filter(Boolean),
          ignoreCase: form.fillBlank.ignoreCase,
          ignoreAccent: form.fillBlank.ignoreAccent,
          trimSpace: form.fillBlank.trimSpace
        }
      };
    }

    return {
      matchingPairs: form.matchingPairs
        .filter((pair) => pair.leftText.trim() && pair.rightText.trim())
        .map((pair, index) => ({
          matchingId: pair.matchingId || `M${index + 1}`,
          leftText: pair.leftText,
          rightText: pair.rightText,
          orderIndex: Number(pair.orderIndex || index + 1)
        }))
    };
  };

  const validateForm = () => {
    if (!selectedBankId) {
      return "Vui lòng chọn ngân hàng câu hỏi";
    }
    if (!form.content.trim()) {
      return "Vui lòng nhập nội dung câu hỏi";
    }
    if (!form.score || Number(form.score) <= 0) {
      return "Điểm câu hỏi phải lớn hơn 0";
    }
    if (form.type === "MULTIPLE_CHOICE") {
      const validOptions = form.options.filter((option) => option.content.trim());
      if (validOptions.length < 2) {
        return "Câu trắc nghiệm cần ít nhất 2 lựa chọn";
      }
      if (!form.options.some((option) => option.isCorrect && option.content.trim())) {
        return "Câu trắc nghiệm cần một đáp án đúng";
      }
    }
    if (form.type === "FILL_BLANK" && !form.fillBlank.acceptedAnswers.trim()) {
      return "Câu điền vào ô trống cần đáp án chấp nhận";
    }
    if (form.type === "MATCHING") {
      const validPairs = form.matchingPairs.filter((pair) => pair.leftText.trim() && pair.rightText.trim());
      if (validPairs.length < 2) {
        return "Câu nối đáp án cần ít nhất 2 cặp";
      }
    }
    return "";
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    const validationMessage = validateForm();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    const payload = {
      type: form.type,
      content: form.content,
      score: Number(form.score),
      difficulty: form.difficulty,
      topic: form.topic,
      status: form.status,
      answer: buildAnswerPayload()
    };

    setSaving(true);
    try {
      if (editing) {
        await teacherService.updateQuestion(form.questionId, payload);
        setMessage("Cập nhật câu hỏi thành công");
      } else {
        await teacherService.createQuestion(selectedBankId, payload);
        setMessage("Tạo câu hỏi thành công");
      }

      resetForm();
      await loadQuestions(selectedBankId, filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu câu hỏi"));
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (question) => {
    const answer = question.answer ?? {};
    setEditing(true);
    setForm({
      ...createInitialForm(),
      questionId: question.questionId,
      type: question.type ?? "MULTIPLE_CHOICE",
      content: question.content ?? "",
      score: String(question.score ?? "1"),
      difficulty: question.difficulty ?? "EASY",
      topic: question.topic ?? "",
      status: question.status ?? "ACTIVE",
      trueFalseAnswer: String(Boolean(answer.correctAnswer)),
      options: answer.options?.length ? answer.options : createDefaultOptions(),
      fillBlank: {
        answerId: answer.fillBlank?.answerId ?? "",
        acceptedAnswers: answer.fillBlank?.acceptedAnswers?.join("|") ?? "",
        ignoreCase: answer.fillBlank?.ignoreCase ?? true,
        ignoreAccent: answer.fillBlank?.ignoreAccent ?? false,
        trimSpace: answer.fillBlank?.trimSpace ?? true
      },
      matchingPairs: answer.matchingPairs?.length
        ? answer.matchingPairs
        : [
            { matchingId: "M1", leftText: "", rightText: "", orderIndex: 1 },
            { matchingId: "M2", leftText: "", rightText: "", orderIndex: 2 }
          ]
    });
  };

  const handleDelete = async (question) => {
    if (!window.confirm("Bạn chắc chắn muốn xóa mềm câu hỏi này?")) {
      return;
    }

    try {
      await teacherService.deleteQuestion(question.questionId);
      setMessage("Đã xóa mềm câu hỏi");
      await loadQuestions(selectedBankId, filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể xóa câu hỏi"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Câu hỏi trong ngân hàng</h2>
        <p>Tạo và quản lý câu hỏi thuộc ngân hàng câu hỏi của giảng viên.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Chọn ngân hàng và lọc câu hỏi</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Ngân hàng
            <select value={selectedBankId} onChange={(event) => setSelectedBankId(event.target.value)}>
              <option value="">Chọn ngân hàng</option>
              {questionBanks.map((bank) => (
                <option key={bank.questionBankId} value={bank.questionBankId}>
                  {bank.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Loại câu hỏi
            <select name="type" value={filters.type} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {QUESTION_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>
          <label>
            Nội dung
            <input name="content" value={filters.content} onChange={updateFilter} />
          </label>
          <label>
            Mức độ
            <select name="difficulty" value={filters.difficulty} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {DIFFICULTIES.map((difficulty) => (
                <option key={difficulty} value={difficulty}>
                  {difficulty}
                </option>
              ))}
            </select>
          </label>
          <label>
            Chủ đề
            <input name="topic" value={filters.topic} onChange={updateFilter} />
          </label>
          <label>
            Trạng thái
            <select name="status" value={filters.status} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {QUESTION_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <div className="form-actions">
            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? "Đang tải..." : "Tìm kiếm"}
            </button>
            <button
              className="secondary-button"
              type="button"
              onClick={() => {
                setFilters(initialFilters);
                loadQuestions(selectedBankId, initialFilters);
              }}
            >
              Xóa lọc
            </button>
          </div>
        </form>
      </section>

      <div className="admin-two-column wide-left">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Danh sách câu hỏi</h3>
            <span className="muted-text">{questions.length} câu hỏi</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Nội dung</th>
                  <th>Loại</th>
                  <th>Điểm</th>
                  <th>Mức độ</th>
                  <th>Chủ đề</th>
                  <th>Trạng thái</th>
                  <th>Cập nhật</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {questions.map((question) => (
                  <tr key={question.questionId}>
                    <td>{question.content}</td>
                    <td>{question.type}</td>
                    <td>{question.score}</td>
                    <td>{question.difficulty}</td>
                    <td>{question.topic || "-"}</td>
                    <td>
                      <span className={`status-pill ${question.status === "ACTIVE" ? "active" : "inactive"}`}>
                        {question.status}
                      </span>
                    </td>
                    <td>{formatDateTime(question.updatedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <button className="text-button" type="button" onClick={() => handleEdit(question)}>
                          Sửa
                        </button>
                        <button className="danger-text-button" type="button" onClick={() => handleDelete(question)}>
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {questions.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="empty-cell">
                      Chưa có câu hỏi trong ngân hàng này.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa câu hỏi" : "Tạo câu hỏi"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Loại câu hỏi
              <select name="type" value={form.type} onChange={updateForm}>
                {QUESTION_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Nội dung
              <textarea name="content" rows="4" value={form.content} onChange={updateForm} required />
            </label>
            <label>
              Điểm
              <input name="score" type="number" min="0.1" step="0.1" value={form.score} onChange={updateForm} />
            </label>
            <label>
              Mức độ
              <select name="difficulty" value={form.difficulty} onChange={updateForm}>
                {DIFFICULTIES.map((difficulty) => (
                  <option key={difficulty} value={difficulty}>
                    {difficulty}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Chủ đề
              <input name="topic" value={form.topic} onChange={updateForm} />
            </label>
            <label>
              Trạng thái
              <select name="status" value={form.status} onChange={updateForm}>
                {QUESTION_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>

            <QuestionAnswerFields
              form={form}
              setCorrectOption={setCorrectOption}
              updateFillBlank={updateFillBlank}
              updateForm={updateForm}
              updateMatchingPair={updateMatchingPair}
              updateOption={updateOption}
              addMatchingPair={addMatchingPair}
              removeMatchingPair={removeMatchingPair}
            />

            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo câu hỏi"}
              </button>
              <button className="secondary-button" type="button" onClick={resetForm}>
                Hủy
              </button>
            </div>
          </form>
        </section>
      </div>
    </section>
  );
}

function QuestionAnswerFields({
  form,
  setCorrectOption,
  updateFillBlank,
  updateForm,
  updateMatchingPair,
  updateOption,
  addMatchingPair,
  removeMatchingPair
}) {
  if (form.type === "TRUE_FALSE") {
    return (
      <label>
        Đáp án đúng
        <select name="trueFalseAnswer" value={form.trueFalseAnswer} onChange={updateForm}>
          <option value="true">Đúng</option>
          <option value="false">Sai</option>
        </select>
      </label>
    );
  }

  if (form.type === "MULTIPLE_CHOICE") {
    return (
      <div className="nested-form-group">
        <h4>Lựa chọn trắc nghiệm</h4>
        {form.options.map((option, index) => (
          <div className="option-row" key={option.optionId}>
            <label>
              Mã
              <input value={option.optionId} onChange={(event) => updateOption(index, "optionId", event.target.value)} />
            </label>
            <label>
              Nội dung
              <input value={option.content} onChange={(event) => updateOption(index, "content", event.target.value)} />
            </label>
            <label className="checkbox-label">
              <input
                type="radio"
                name="correctOption"
                checked={option.isCorrect}
                onChange={() => setCorrectOption(option.optionId)}
              />
              Đáp án đúng
            </label>
          </div>
        ))}
      </div>
    );
  }

  if (form.type === "FILL_BLANK") {
    return (
      <div className="nested-form-group">
        <h4>Đáp án điền vào ô trống</h4>
        <label>
          Answer ID
          <input name="answerId" value={form.fillBlank.answerId} onChange={updateFillBlank} />
        </label>
        <label>
          Đáp án chấp nhận
          <input
            name="acceptedAnswers"
            value={form.fillBlank.acceptedAnswers}
            onChange={updateFillBlank}
            placeholder="HyperText Transfer Protocol|Hyper Text Transfer Protocol"
          />
        </label>
        <label className="checkbox-label">
          <input name="ignoreCase" type="checkbox" checked={form.fillBlank.ignoreCase} onChange={updateFillBlank} />
          Không phân biệt hoa thường
        </label>
        <label className="checkbox-label">
          <input name="ignoreAccent" type="checkbox" checked={form.fillBlank.ignoreAccent} onChange={updateFillBlank} />
          Bỏ dấu tiếng Việt khi so sánh
        </label>
        <label className="checkbox-label">
          <input name="trimSpace" type="checkbox" checked={form.fillBlank.trimSpace} onChange={updateFillBlank} />
          Bỏ khoảng trắng đầu cuối
        </label>
      </div>
    );
  }

  return (
    <div className="nested-form-group">
      <div className="panel-heading compact-heading">
        <h4>Cặp nối đáp án</h4>
        <button className="text-button" type="button" onClick={addMatchingPair}>
          Thêm cặp
        </button>
      </div>
      {form.matchingPairs.map((pair, index) => (
        <div className="matching-row" key={`${pair.matchingId}-${index}`}>
          <label>
            Mã
            <input value={pair.matchingId} onChange={(event) => updateMatchingPair(index, "matchingId", event.target.value)} />
          </label>
          <label>
            Vế trái
            <input value={pair.leftText} onChange={(event) => updateMatchingPair(index, "leftText", event.target.value)} />
          </label>
          <label>
            Vế phải
            <input value={pair.rightText} onChange={(event) => updateMatchingPair(index, "rightText", event.target.value)} />
          </label>
          <button className="danger-text-button" type="button" onClick={() => removeMatchingPair(index)}>
            Xóa
          </button>
        </div>
      ))}
    </div>
  );
}
