import { useEffect, useMemo, useState } from "react";
import { teacherService } from "../../services/teacherService.js";
import { formatNumber, getClassLabel, getSubjectLabel, resolveErrorMessage } from "./teacherUtils.js";

export function TeacherStatisticsPage() {
  const [classes, setClasses] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [exams, setExams] = useState([]);
  const [examId, setExamId] = useState("");
  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [examStats, setExamStats] = useState(null);
  const [classStats, setClassStats] = useState(null);
  const [subjectStats, setSubjectStats] = useState(null);
  const [loading, setLoading] = useState("");
  const [error, setError] = useState("");

  const subjects = useMemo(() => {
    const map = new Map();
    assignments.forEach((assignment) => {
      if (assignment.subjectId && assignment.subject) {
        map.set(assignment.subjectId, assignment.subject);
      }
    });
    return [...map.values()];
  }, [assignments]);

  useEffect(() => {
    async function loadOptions() {
      setError("");
      try {
        const [classList, assignmentList, examList] = await Promise.all([
          teacherService.getMyClasses(),
          teacherService.getMyAssignments(),
          teacherService.searchExams({})
        ]);
        setClasses(classList ?? []);
        setAssignments(assignmentList ?? []);
        setExams(examList ?? []);
      } catch (err) {
        setError(resolveErrorMessage(err, "Không thể tải dữ liệu chọn thống kê"));
      }
    }

    loadOptions();
  }, []);

  const loadExamStats = async (event) => {
    event.preventDefault();
    if (!examId) {
      setError("Vui lòng chọn bài thi");
      return;
    }

    setLoading("exam");
    setError("");
    try {
      const data = await teacherService.getExamStatistics(examId);
      setExamStats(data);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải thống kê bài thi"));
    } finally {
      setLoading("");
    }
  };

  const loadClassStats = async (event) => {
    event.preventDefault();
    if (!classId) {
      setError("Vui lòng chọn lớp");
      return;
    }

    setLoading("class");
    setError("");
    try {
      const data = await teacherService.getClassStatistics(classId);
      setClassStats(data);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải thống kê lớp"));
    } finally {
      setLoading("");
    }
  };

  const loadSubjectStats = async (event) => {
    event.preventDefault();
    if (!subjectId) {
      setError("Vui lòng chọn môn thi");
      return;
    }

    setLoading("subject");
    setError("");
    try {
      const data = await teacherService.getSubjectStatistics(subjectId);
      setSubjectStats(data);
    } catch (err) {
      setError(resolveErrorMessage(err, "Không thể tải thống kê môn thi"));
    } finally {
      setLoading("");
    }
  };

  return (
    <section className="admin-page">
      <div className="section-heading">
        <h2>Thống kê điểm</h2>
        <p>Xem thống kê điểm theo bài thi, lớp và môn thi thuộc phạm vi giảng viên phụ trách.</p>
      </div>

      {error ? <p className="error-message">{error}</p> : null}

      <div className="admin-three-column">
        <section className="admin-panel">
          <h3>Theo bài thi</h3>
          <form className="stacked-form" onSubmit={loadExamStats}>
            <label>
              Bài thi
              <select value={examId} onChange={(event) => setExamId(event.target.value)}>
                <option value="">Chọn bài thi</option>
                {exams.map((exam) => (
                  <option key={exam.examId} value={exam.examId}>
                    {exam.examName}
                  </option>
                ))}
              </select>
            </label>
            <button className="primary-button" type="submit" disabled={loading === "exam"}>
              {loading === "exam" ? "Đang tải..." : "Xem thống kê"}
            </button>
          </form>
        </section>

        <section className="admin-panel">
          <h3>Theo lớp</h3>
          <form className="stacked-form" onSubmit={loadClassStats}>
            <label>
              Lớp
              <select value={classId} onChange={(event) => setClassId(event.target.value)}>
                <option value="">Chọn lớp</option>
                {classes.map((schoolClass) => (
                  <option key={schoolClass.classId} value={schoolClass.classId}>
                    {getClassLabel(schoolClass)}
                  </option>
                ))}
              </select>
            </label>
            <button className="primary-button" type="submit" disabled={loading === "class"}>
              {loading === "class" ? "Đang tải..." : "Xem thống kê"}
            </button>
          </form>
        </section>

        <section className="admin-panel">
          <h3>Theo môn thi</h3>
          <form className="stacked-form" onSubmit={loadSubjectStats}>
            <label>
              Môn thi
              <select value={subjectId} onChange={(event) => setSubjectId(event.target.value)}>
                <option value="">Chọn môn thi</option>
                {subjects.map((subject) => (
                  <option key={subject.subjectId} value={subject.subjectId}>
                    {getSubjectLabel(subject)}
                  </option>
                ))}
              </select>
            </label>
            <button className="primary-button" type="submit" disabled={loading === "subject"}>
              {loading === "subject" ? "Đang tải..." : "Xem thống kê"}
            </button>
          </form>
        </section>
      </div>

      {examStats ? <ExamStatisticsPanel data={examStats} /> : null}
      {classStats ? <ClassStatisticsPanel data={classStats} /> : null}
      {subjectStats ? <SubjectStatisticsPanel data={subjectStats} /> : null}
    </section>
  );
}

function ExamStatisticsPanel({ data }) {
  return (
    <section className="admin-panel">
      <div className="panel-heading">
        <h3>Thống kê bài thi: {data.examName}</h3>
        <span className="muted-text">{data.subjectName || data.subjectId}</span>
      </div>
      <div className="summary-grid compact-summary">
        <Metric label="Sinh viên được giao" value={data.assignedStudentCount} />
        <Metric label="Đã làm" value={data.attemptedStudentCount} />
        <Metric label="Chưa làm" value={data.notStartedStudentCount} />
        <Metric label="Đã nộp" value={data.submittedAttemptCount} />
        <Metric label="Quá hạn" value={data.expiredAttemptCount} />
        <Metric label="Cao nhất" value={formatNumber(data.highestScore)} />
        <Metric label="Thấp nhất" value={formatNumber(data.lowestScore)} />
        <Metric label="Trung bình" value={formatNumber(data.averageScore)} />
      </div>
      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Mã sinh viên</th>
              <th>Họ tên</th>
              <th>Email</th>
              <th>Số lần làm</th>
              <th>Trạng thái mới nhất</th>
              <th>Điểm mới nhất</th>
              <th>Điểm tốt nhất</th>
            </tr>
          </thead>
          <tbody>
            {(data.studentScores ?? []).map((student) => (
              <tr key={student.studentId}>
                <td>{student.studentCode}</td>
                <td>{student.fullName}</td>
                <td>{student.email}</td>
                <td>{student.attemptCount}</td>
                <td>{student.latestStatus || "-"}</td>
                <td>{formatNumber(student.latestScore)}</td>
                <td>{formatNumber(student.bestScore)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function ClassStatisticsPanel({ data }) {
  return (
    <section className="admin-panel">
      <div className="panel-heading">
        <h3>Thống kê lớp: {data.className}</h3>
        <span className="muted-text">{data.classCode}</span>
      </div>
      <div className="summary-grid compact-summary">
        <Metric label="Sinh viên" value={data.studentCount} />
        <Metric label="Bài thi" value={data.examCount} />
        <Metric label="Điểm trung bình lớp" value={formatNumber(data.classAverageScore)} />
      </div>
      <div className="chart-list">
        {(data.scoreChart ?? []).map((bucket) => (
          <div key={bucket.label} className="chart-row">
            <span>{bucket.label}</span>
            <div>
              <i style={{ width: `${Math.min(bucket.count * 12, 100)}%` }} />
            </div>
            <strong>{bucket.count}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function SubjectStatisticsPanel({ data }) {
  return (
    <section className="admin-panel">
      <div className="panel-heading">
        <h3>Thống kê môn thi: {data.subjectName}</h3>
        <span className="muted-text">{data.subjectCode}</span>
      </div>
      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Bài thi</th>
              <th>Sinh viên được giao</th>
              <th>Đã làm</th>
              <th>Điểm trung bình</th>
            </tr>
          </thead>
          <tbody>
            {(data.exams ?? []).map((exam) => (
              <tr key={exam.examId}>
                <td>{exam.examName}</td>
                <td>{exam.assignedStudentCount}</td>
                <td>{exam.attemptedStudentCount}</td>
                <td>{formatNumber(exam.averageScore)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function Metric({ label, value }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
