import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, formatNumber, getClassLabel, getSubjectLabel, resolveErrorMessage } from "./teacherUtils.js";

const EXAM_STATUSES = ["DRAFT", "PUBLISHED", "OPEN", "CLOSED", "CANCELLED"];
const RESULT_STATUSES = ["NOT_PUBLISHED", "PUBLISHED"];

const initialFilters = {
  examName: "",
  classId: "",
  subjectId: "",
  questionBankId: "",
  status: "",
  resultStatus: ""
};

const initialForm = {
  examId: "",
  examName: "",
  classIds: [],
  subjectId: "",
  questionBankId: "",
  durationMinutes: "60",
  maxAttempts: "1",
  allowViewScore: true,
  allowReview: false,
  allowViewCorrectAnswer: false,
  shuffleQuestions: true,
  shuffleOptions: true,
  examPassword: "",
  removePassword: false
};

export function TeacherExamsPage() {
  const [filters, setFilters] = useState(initialFilters);
  const [form, setForm] = useState(initialForm);
  const [classes, setClasses] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [questionBanks, setQuestionBanks] = useState([]);
  const [exams, setExams] = useState([]);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const subjects = useMemo(() => {
    const map = new Map();
    assignments.forEach((assignment) => {
      if (assignment.subjectId && assignment.subject) {
        map.set(assignment.subjectId, assignment.subject);
      }
    });
    return [...map.values()];
  }, [assignments]);

  const loadExams = async (nextFilters = filters) => {
    setLoading(true);
    setError("");
    try {
      const data = await teacherService.searchExams(nextFilters);
      setExams(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải danh sách bài thi"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    async function loadInitialData() {
      try {
        const [classList, assignmentList, bankList] = await Promise.all([
          teacherService.getMyClasses(),
          teacherService.getMyAssignments(),
          teacherService.getMyQuestionBanks()
        ]);
        setClasses(classList ?? []);
        setAssignments(assignmentList ?? []);
        setQuestionBanks(bankList ?? []);
        await loadExams(initialFilters);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải dữ liệu bài thi"));
      }
    }

    loadInitialData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateFilter = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const updateForm = (event) => {
    const { name, value, type, checked, selectedOptions } = event.target;
    if (name === "classIds") {
      setForm((current) => ({
        ...current,
        classIds: Array.from(selectedOptions).map((option) => option.value)
      }));
      return;
    }
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  };

  const resetForm = () => {
    setEditing(false);
    setForm(initialForm);
  };

  const handleSearch = (event) => {
    event.preventDefault();
    loadExams(filters);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    loadExams(initialFilters);
  };

  const validateForm = () => {
    if (!form.examName.trim()) {
      return "Vui lòng nhập tên bài thi";
    }
    if (!form.classIds.length) {
      return "Vui lòng chọn ít nhất một lớp";
    }
    if (!form.subjectId || !form.questionBankId) {
      return "Vui lòng chọn môn thi và ngân hàng câu hỏi";
    }
    if (Number(form.durationMinutes) <= 0 || Number(form.maxAttempts) <= 0) {
      return "Thời gian làm bài và số lần làm tối đa phải lớn hơn 0";
    }
    return "";
  };

  const buildPayload = () => {
    const payload = {
      examName: form.examName,
      classIds: form.classIds,
      subjectId: form.subjectId,
      questionBankId: form.questionBankId,
      durationMinutes: Number(form.durationMinutes),
      maxAttempts: Number(form.maxAttempts),
      allowViewScore: form.allowViewScore,
      allowReview: form.allowReview,
      allowViewCorrectAnswer: form.allowViewCorrectAnswer,
      shuffleQuestions: form.shuffleQuestions,
      shuffleOptions: form.shuffleOptions
    };

    if (form.examPassword.trim()) {
      payload.examPassword = form.examPassword;
    }
    if (editing && form.removePassword) {
      payload.removePassword = true;
    }
    return payload;
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

    setSaving(true);
    try {
      if (editing) {
        await teacherService.updateExam(form.examId, buildPayload());
        setMessage("Cập nhật bài thi thành công");
      } else {
        await teacherService.createExam(buildPayload());
        setMessage("Tạo bài thi thành công");
      }
      resetForm();
      await loadExams(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu bài thi"));
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (exam) => {
    setEditing(true);
    setForm({
      ...initialForm,
      examId: exam.examId,
      examName: exam.examName ?? "",
      classIds: exam.classIds ?? [],
      subjectId: exam.subjectId ?? "",
      questionBankId: exam.questionBankId ?? "",
      durationMinutes: String(exam.durationMinutes ?? 60),
      maxAttempts: String(exam.maxAttempts ?? 1),
      allowViewScore: Boolean(exam.allowViewScore),
      allowReview: Boolean(exam.allowReview),
      allowViewCorrectAnswer: Boolean(exam.allowViewCorrectAnswer),
      shuffleQuestions: Boolean(exam.shuffleQuestions),
      shuffleOptions: Boolean(exam.shuffleOptions)
    });
  };

  const runExamAction = async (label, action) => {
    if (!window.confirm(`Bạn chắc chắn muốn ${label}?`)) {
      return;
    }

    setError("");
    setMessage("");
    try {
      await action();
      setMessage("Thao tác bài thi thành công");
      await loadExams(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể thực hiện thao tác bài thi"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Bài thi</h2>
        <p>Tạo, chỉnh sửa, công bố, đóng hoặc hủy bài thi thuộc phạm vi giảng viên.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Tìm kiếm</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Tên bài thi
            <input name="examName" value={filters.examName} onChange={updateFilter} />
          </label>
          <label>
            Lớp
            <select name="classId" value={filters.classId} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {classes.map((schoolClass) => (
                <option key={schoolClass.classId} value={schoolClass.classId}>
                  {getClassLabel(schoolClass)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Môn thi
            <select name="subjectId" value={filters.subjectId} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {subjects.map((subject) => (
                <option key={subject.subjectId} value={subject.subjectId}>
                  {getSubjectLabel(subject)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Ngân hàng
            <select name="questionBankId" value={filters.questionBankId} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {questionBanks.map((bank) => (
                <option key={bank.questionBankId} value={bank.questionBankId}>
                  {bank.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Trạng thái
            <select name="status" value={filters.status} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {EXAM_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <label>
            Kết quả
            <select name="resultStatus" value={filters.resultStatus} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {RESULT_STATUSES.map((status) => (
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
            <button className="secondary-button" type="button" onClick={handleResetFilters}>
              Xóa lọc
            </button>
          </div>
        </form>
      </section>

      <div className="admin-two-column wide-left">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Danh sách bài thi</h3>
            <span className="muted-text">{exams.length} bài thi</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Tên bài thi</th>
                  <th>Môn</th>
                  <th>Lớp</th>
                  <th>Thời gian</th>
                  <th>Tổng điểm</th>
                  <th>Trạng thái</th>
                  <th>Kết quả</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {exams.map((exam) => (
                  <tr key={exam.examId}>
                    <td>{exam.examName}</td>
                    <td>{exam.subject?.subjectName || exam.subjectId}</td>
                    <td>{(exam.classes ?? []).map(getClassLabel).join(", ") || exam.classIds?.join(", ")}</td>
                    <td>{exam.durationMinutes} phút</td>
                    <td>{formatNumber(exam.totalScore)}</td>
                    <td>
                      <span className={`status-pill ${["PUBLISHED", "OPEN"].includes(exam.status) ? "active" : "inactive"}`}>
                        {exam.status}
                      </span>
                    </td>
                    <td>{exam.resultStatus}</td>
                    <td>
                      <div className="row-actions">
                        <button className="text-button" type="button" onClick={() => handleEdit(exam)}>
                          Sửa
                        </button>
                        <Link className="text-link" to={`/teacher/exams/${exam.examId}/questions`}>
                          Câu hỏi
                        </Link>
                        <Link className="text-link" to={`/teacher/exams/${exam.examId}/sessions`}>
                          Ca thi
                        </Link>
                        <Link className="text-link" to={`/teacher/exams/${exam.examId}/attempts`}>
                          Bài làm
                        </Link>
                        <button className="text-button" type="button" onClick={() => runExamAction("công bố bài thi", () => teacherService.publishExam(exam.examId))}>
                          Công bố
                        </button>
                        <button className="text-button" type="button" onClick={() => runExamAction("đóng bài thi", () => teacherService.closeExam(exam.examId))}>
                          Đóng
                        </button>
                        <button className="danger-text-button" type="button" onClick={() => runExamAction("hủy bài thi", () => teacherService.cancelExam(exam.examId))}>
                          Hủy
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {exams.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="empty-cell">
                      Chưa có bài thi.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa bài thi" : "Tạo bài thi"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Tên bài thi
              <input name="examName" value={form.examName} onChange={updateForm} required />
            </label>
            <label>
              Lớp được giao
              <select name="classIds" multiple size="5" value={form.classIds} onChange={updateForm} required>
                {classes.map((schoolClass) => (
                  <option key={schoolClass.classId} value={schoolClass.classId}>
                    {getClassLabel(schoolClass)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Môn thi
              <select name="subjectId" value={form.subjectId} onChange={updateForm} required>
                <option value="">Chọn môn thi</option>
                {subjects.map((subject) => (
                  <option key={subject.subjectId} value={subject.subjectId}>
                    {getSubjectLabel(subject)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Ngân hàng câu hỏi
              <select name="questionBankId" value={form.questionBankId} onChange={updateForm} required>
                <option value="">Chọn ngân hàng</option>
                {questionBanks.map((bank) => (
                  <option key={bank.questionBankId} value={bank.questionBankId}>
                    {bank.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Thời gian làm bài phút
              <input name="durationMinutes" type="number" min="1" value={form.durationMinutes} onChange={updateForm} />
            </label>
            <label>
              Số lần làm tối đa
              <input name="maxAttempts" type="number" min="1" value={form.maxAttempts} onChange={updateForm} />
            </label>
            <label>
              Mật khẩu bài thi
              <input name="examPassword" type="password" value={form.examPassword} onChange={updateForm} autoComplete="new-password" />
            </label>
            {editing ? (
              <label className="checkbox-label">
                <input name="removePassword" type="checkbox" checked={form.removePassword} onChange={updateForm} />
                Xóa mật khẩu bài thi
              </label>
            ) : null}
            <label className="checkbox-label">
              <input name="allowViewScore" type="checkbox" checked={form.allowViewScore} onChange={updateForm} />
              Cho xem điểm
            </label>
            <label className="checkbox-label">
              <input name="allowReview" type="checkbox" checked={form.allowReview} onChange={updateForm} />
              Cho review bài làm
            </label>
            <label className="checkbox-label">
              <input name="allowViewCorrectAnswer" type="checkbox" checked={form.allowViewCorrectAnswer} onChange={updateForm} />
              Cho xem đáp án đúng
            </label>
            <label className="checkbox-label">
              <input name="shuffleQuestions" type="checkbox" checked={form.shuffleQuestions} onChange={updateForm} />
              Đảo câu hỏi
            </label>
            <label className="checkbox-label">
              <input name="shuffleOptions" type="checkbox" checked={form.shuffleOptions} onChange={updateForm} />
              Đảo đáp án
            </label>
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo bài thi"}
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
