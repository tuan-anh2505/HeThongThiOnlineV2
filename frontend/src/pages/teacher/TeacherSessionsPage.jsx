import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { teacherService } from "../../services/teacherService.js";
import { formatDateTime, fromDateTimeLocal, resolveErrorMessage, toDateTimeLocal } from "./teacherUtils.js";

const initialForm = {
  examSessionId: "",
  startTime: "",
  endTime: ""
};

export function TeacherSessionsPage() {
  const { examId } = useParams();
  const [exam, setExam] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      const [examData, sessionList] = await Promise.all([
        teacherService.getExam(examId),
        teacherService.getExamSessions(examId)
      ]);
      setExam(examData);
      setSessions(sessionList ?? []);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải ca thi"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [examId]);

  const updateForm = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const resetForm = () => {
    setEditing(false);
    setForm(initialForm);
  };

  const handleEdit = (session) => {
    setEditing(true);
    setForm({
      examSessionId: session.examSessionId,
      startTime: toDateTimeLocal(session.startTime),
      endTime: toDateTimeLocal(session.endTime)
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!form.startTime || !form.endTime) {
      setError("Vui lòng nhập thời gian bắt đầu và kết thúc");
      return;
    }
    if (new Date(form.startTime) >= new Date(form.endTime)) {
      setError("Thời gian kết thúc phải sau thời gian bắt đầu");
      return;
    }

    const payload = {
      startTime: fromDateTimeLocal(form.startTime),
      endTime: fromDateTimeLocal(form.endTime)
    };

    setSaving(true);
    try {
      if (editing) {
        await teacherService.updateExamSession(form.examSessionId, payload);
        setMessage("Cập nhật ca thi thành công");
      } else {
        await teacherService.createExamSession(examId, payload);
        setMessage("Tạo ca thi thành công");
      }
      resetForm();
      await loadData();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể lưu ca thi"));
    } finally {
      setSaving(false);
    }
  };

  const runSessionAction = async (label, action) => {
    if (!window.confirm(`Bạn chắc chắn muốn ${label}?`)) {
      return;
    }

    setError("");
    setMessage("");
    try {
      await action();
      setMessage("Thao tác ca thi thành công");
      await loadData();
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể thao tác ca thi"));
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Ca thi</h2>
        <p>Tạo và lên lịch ca thi cho bài thi đã chọn.</p>
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
      </section>

      <div className="admin-two-column">
        <section className="admin-panel">
          <div className="panel-heading">
            <h3>Danh sách ca thi</h3>
            <span className="muted-text">{loading ? "Đang tải..." : `${sessions.length} ca thi`}</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Bắt đầu</th>
                  <th>Kết thúc</th>
                  <th>Trạng thái</th>
                  <th>Cập nhật</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((session) => (
                  <tr key={session.examSessionId}>
                    <td>{formatDateTime(session.startTime)}</td>
                    <td>{formatDateTime(session.endTime)}</td>
                    <td>
                      <span className={`status-pill ${session.status === "IN_PROGRESS" ? "active" : "inactive"}`}>
                        {session.status}
                      </span>
                    </td>
                    <td>{formatDateTime(session.updatedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <button className="text-button" type="button" onClick={() => handleEdit(session)}>
                          Sửa
                        </button>
                        <button
                          className="text-button"
                          type="button"
                          onClick={() =>
                            runSessionAction("hủy ca thi", () => teacherService.cancelExamSession(session.examSessionId))
                          }
                        >
                          Hủy ca
                        </button>
                        <button
                          className="danger-text-button"
                          type="button"
                          onClick={() =>
                            runSessionAction("xóa ca thi", () => teacherService.deleteExamSession(session.examSessionId))
                          }
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {sessions.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="empty-cell">
                      Chưa có ca thi.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel">
          <div className="panel-heading">
            <h3>{editing ? "Sửa ca thi" : "Tạo ca thi"}</h3>
            {editing ? (
              <button className="text-button" type="button" onClick={resetForm}>
                Tạo mới
              </button>
            ) : null}
          </div>

          <form className="stacked-form" onSubmit={handleSubmit}>
            <label>
              Thời gian bắt đầu
              <input name="startTime" type="datetime-local" value={form.startTime} onChange={updateForm} required />
            </label>
            <label>
              Thời gian kết thúc
              <input name="endTime" type="datetime-local" value={form.endTime} onChange={updateForm} required />
            </label>
            <div className="form-actions">
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo ca thi"}
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
