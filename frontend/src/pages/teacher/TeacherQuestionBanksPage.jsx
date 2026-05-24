import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, getSubjectLabel, resolveErrorMessage } from "./teacherUtils.js";

const QUESTION_BANK_STATUSES = ["ACTIVE", "INACTIVE"];

const initialFilters = {
  name: "",
  subjectId: "",
  status: ""
};

const initialForm = {
  questionBankId: "",
  name: "",
  description: "",
  subjectId: "",
  status: "ACTIVE"
};

export function TeacherQuestionBanksPage() {
  const [filters, setFilters] = useState(initialFilters);
  const [form, setForm] = useState(initialForm);
  const [questionBanks, setQuestionBanks] = useState([]);
  const [assignments, setAssignments] = useState([]);
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

  const loadQuestionBanks = async (nextFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await teacherService.searchQuestionBanks(nextFilters);
      setQuestionBanks(data ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải ngân hàng câu hỏi"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    async function loadInitialData() {
      try {
        const [assignmentList] = await Promise.all([teacherService.getMyAssignments(), loadQuestionBanks(initialFilters)]);
        setAssignments(assignmentList ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải dữ liệu ngân hàng câu hỏi"));
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
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const resetForm = () => {
    setEditing(false);
    setForm(initialForm);
  };

  const handleSearch = (event) => {
    event.preventDefault();
    loadQuestionBanks(filters);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    loadQuestionBanks(initialFilters);
  };

  const handleEdit = (questionBank) => {
    setEditing(true);
    setForm({
      questionBankId: questionBank.questionBankId,
      name: questionBank.name ?? "",
      description: questionBank.description ?? "",
      subjectId: questionBank.subjectId ?? "",
      status: questionBank.status ?? "ACTIVE"
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!form.name.trim() || !form.subjectId) {
      setError("Vui lòng nhập tên ngân hàng và chọn môn thi");
      return;
    }

    const payload = {
      name: form.name,
      description: form.description,
      subjectId: form.subjectId,
      status: form.status
    };

    setSaving(true);
    try {
      if (editing) {
        await teacherService.updateQuestionBank(form.questionBankId, payload);
        setMessage("Cập nhật ngân hàng câu hỏi thành công");
      } else {
        await teacherService.createQuestionBank(payload);
        setMessage("Tạo ngân hàng câu hỏi thành công");
      }

      resetForm();
      await loadQuestionBanks(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu ngân hàng câu hỏi"));
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (questionBank) => {
    const isActive = questionBank.status === "ACTIVE";
    const actionLabel = isActive ? "ngừng hoạt động" : "khôi phục";
    if (!window.confirm(`Bạn có chắc muốn ${actionLabel} dữ liệu này không?`)) {
      return;
    }

    try {
      if (isActive) {
        await teacherService.deactivateQuestionBank(questionBank.questionBankId);
        setMessage("Đã ngừng hoạt động ngân hàng câu hỏi");
      } else {
        await teacherService.activateQuestionBank(questionBank.questionBankId);
        setMessage("Đã khôi phục ngân hàng câu hỏi");
      }
      await loadQuestionBanks(filters);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể cập nhật trạng thái ngân hàng câu hỏi"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Ngân hàng câu hỏi</h2>
        <p>Giảng viên quản lý ngân hàng câu hỏi của mình theo môn thi được phân công.</p>
      </div>

      {message ? <p className="success-message">{message}</p> : null}
      {error ? <p className="error-message">{error}</p> : null}

      <section className="admin-panel">
        <h3>Tìm kiếm</h3>
        <form className="filter-form" onSubmit={handleSearch}>
          <label>
            Tên ngân hàng
            <input name="name" value={filters.name} onChange={updateFilter} />
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
            Trạng thái
            <select name="status" value={filters.status} onChange={updateFilter}>
              <option value="">Tất cả</option>
              {QUESTION_BANK_STATUSES.map((status) => (
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

      <div className="admin-two-column">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Danh sách ngân hàng</h3>
            <span className="muted-text">{questionBanks.length} ngân hàng</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Tên ngân hàng</th>
                  <th>Môn thi</th>
                  <th>Trạng thái</th>
                  <th>Cập nhật</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {questionBanks.map((questionBank) => (
                  <tr key={questionBank.questionBankId}>
                    <td>{questionBank.name}</td>
                    <td>{questionBank.subject?.subjectName || questionBank.subjectId}</td>
                    <td>
                      <span className={`status-pill ${questionBank.status === "ACTIVE" ? "active" : "inactive"}`}>
                        {questionBank.status}
                      </span>
                    </td>
                    <td>{formatDateTime(questionBank.updatedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <Link className="text-link" to={`/teacher/question-banks/${questionBank.questionBankId}/questions`}>
                          Câu hỏi
                        </Link>
                        <button className="text-button" type="button" onClick={() => handleEdit(questionBank)}>
                          Sửa
                        </button>
                        <button
                          className={questionBank.status === "ACTIVE" ? "danger-text-button" : "text-button"}
                          type="button"
                          onClick={() => handleStatusChange(questionBank)}
                        >
                          {questionBank.status === "ACTIVE" ? "Ngừng hoạt động" : "Khôi phục"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {questionBanks.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="empty-cell">
                      Chưa có ngân hàng câu hỏi.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa ngân hàng" : "Tạo ngân hàng"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Tên ngân hàng
              <input name="name" value={form.name} onChange={updateForm} required />
            </label>
            <label>
              Mô tả
              <textarea name="description" rows="4" value={form.description} onChange={updateForm} />
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
              Trạng thái
              <select name="status" value={form.status} onChange={updateForm}>
                {QUESTION_BANK_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo ngân hàng"}
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
