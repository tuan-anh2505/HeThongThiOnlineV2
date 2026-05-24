import { apiRequest } from "./apiClient.js";

function buildQuery(params = {}) {
  const query = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return;
    }

    const normalizedValue = typeof value === "string" ? value.trim() : value;
    if (normalizedValue === "") {
      return;
    }

    query.set(key, normalizedValue);
  });

  const queryString = query.toString();
  return queryString ? `?${queryString}` : "";
}

function cleanPayload(value) {
  if (Array.isArray(value)) {
    return value.map(cleanPayload);
  }

  if (value && typeof value === "object") {
    return Object.entries(value).reduce((result, [key, currentValue]) => {
      const nextValue = cleanPayload(currentValue);
      if (nextValue !== undefined) {
        result[key] = nextValue;
      }
      return result;
    }, {});
  }

  if (typeof value === "string") {
    return value.trim();
  }

  return value === null || value === undefined ? undefined : value;
}

function requestWithBody(path, method, payload) {
  return apiRequest(path, {
    method,
    body: cleanPayload(payload) ?? {}
  });
}

export const teacherService = {
  getMyClasses() {
    return apiRequest("/api/teacher/classes");
  },

  getClassStudents(classId) {
    return apiRequest(`/api/classes/${classId}/students`);
  },

  addStudentToClass(classId, studentId) {
    return requestWithBody(`/api/classes/${classId}/students`, "POST", { studentId });
  },

  removeStudentFromClass(classId, studentId) {
    return apiRequest(`/api/classes/${classId}/students/${studentId}`, { method: "DELETE" });
  },

  getMyAssignments() {
    return apiRequest("/api/teacher/subjects");
  },

  getMyQuestionBanks() {
    return apiRequest("/api/teacher/question-banks");
  },

  searchQuestionBanks(filters) {
    return apiRequest(`/api/question-banks${buildQuery(filters)}`);
  },

  createQuestionBank(payload) {
    return requestWithBody("/api/question-banks", "POST", payload);
  },

  updateQuestionBank(id, payload) {
    return requestWithBody(`/api/question-banks/${id}`, "PUT", payload);
  },

  deleteQuestionBank(id) {
    return apiRequest(`/api/question-banks/${id}`, { method: "DELETE" });
  },

  deactivateQuestionBank(id) {
    return apiRequest(`/api/question-banks/${id}/deactivate`, { method: "PATCH" });
  },

  activateQuestionBank(id) {
    return apiRequest(`/api/question-banks/${id}/activate`, { method: "PATCH" });
  },

  searchQuestions(bankId, filters) {
    return apiRequest(`/api/question-banks/${bankId}/questions${buildQuery(filters)}`);
  },

  importQuestionsFromFile(bankId, file, sourceType = "AUTO") {
    const formData = new FormData();
    formData.append("file", file);
    return apiRequest(`/api/question-banks/${bankId}/import${buildQuery({ sourceType })}`, {
      method: "POST",
      body: formData
    });
  },

  importQuestionsFromUrl(bankId, payload) {
    return requestWithBody(`/api/question-banks/${bankId}/import-from-url`, "POST", payload);
  },

  previewAiQuestionsFromFile(bankId, file, sourceType = "AUTO") {
    const formData = new FormData();
    formData.append("file", file);
    return apiRequest(`/api/question-banks/${bankId}/ai-import/file/preview${buildQuery({ sourceType })}`, {
      method: "POST",
      body: formData
    });
  },

  previewAiQuestionsFromUrl(bankId, payload) {
    return requestWithBody(`/api/question-banks/${bankId}/ai-import/url/preview`, "POST", payload);
  },

  commitAiQuestions(bankId, questions) {
    return requestWithBody(`/api/question-banks/${bankId}/ai-import/commit`, "POST", { questions });
  },

  createQuestion(bankId, payload) {
    return requestWithBody(`/api/question-banks/${bankId}/questions`, "POST", payload);
  },

  updateQuestion(questionId, payload) {
    return requestWithBody(`/api/questions/${questionId}`, "PUT", payload);
  },

  deleteQuestion(questionId) {
    return apiRequest(`/api/questions/${questionId}`, { method: "DELETE" });
  },

  deactivateQuestion(questionId) {
    return apiRequest(`/api/questions/${questionId}/deactivate`, { method: "PATCH" });
  },

  activateQuestion(questionId) {
    return apiRequest(`/api/questions/${questionId}/activate`, { method: "PATCH" });
  },

  searchExams(filters) {
    return apiRequest(`/api/exams${buildQuery(filters)}`);
  },

  createExam(payload) {
    return requestWithBody("/api/exams", "POST", payload);
  },

  updateExam(id, payload) {
    return requestWithBody(`/api/exams/${id}`, "PUT", payload);
  },

  deleteExam(id) {
    return apiRequest(`/api/exams/${id}`, { method: "DELETE" });
  },

  publishExam(id) {
    return apiRequest(`/api/exams/${id}/publish`, { method: "PATCH" });
  },

  closeExam(id) {
    return apiRequest(`/api/exams/${id}/close`, { method: "PATCH" });
  },

  cancelExam(id) {
    return apiRequest(`/api/exams/${id}/cancel`, { method: "PATCH" });
  },

  getExam(id) {
    return apiRequest(`/api/exams/${id}`);
  },

  addQuestionToExam(examId, payload) {
    return requestWithBody(`/api/exams/${examId}/questions`, "POST", payload);
  },

  removeQuestionFromExam(examId, questionId) {
    return apiRequest(`/api/exams/${examId}/questions/${questionId}`, { method: "DELETE" });
  },

  generateRandomQuestions(examId, configs) {
    return requestWithBody(`/api/exams/${examId}/generate-random-questions`, "POST", { configs });
  },

  getExamSessions(examId) {
    return apiRequest(`/api/exams/${examId}/sessions`);
  },

  createExamSession(examId, payload) {
    return requestWithBody(`/api/exams/${examId}/sessions`, "POST", payload);
  },

  updateExamSession(sessionId, payload) {
    return requestWithBody(`/api/exam-sessions/${sessionId}`, "PUT", payload);
  },

  deleteExamSession(sessionId) {
    return apiRequest(`/api/exam-sessions/${sessionId}`, { method: "DELETE" });
  },

  cancelExamSession(sessionId) {
    return apiRequest(`/api/exam-sessions/${sessionId}/cancel`, { method: "PATCH" });
  },

  getExamAttempts(examId) {
    return apiRequest(`/api/teacher/exams/${examId}/attempts`);
  },

  getAttemptDetail(attemptId) {
    return apiRequest(`/api/teacher/attempts/${attemptId}`);
  },

  publishResults(examId) {
    return apiRequest(`/api/teacher/exams/${examId}/publish-results`, { method: "PATCH" });
  },

  hideResults(examId) {
    return apiRequest(`/api/teacher/exams/${examId}/hide-results`, { method: "PATCH" });
  },

  getExamStatistics(examId) {
    return apiRequest(`/api/teacher/statistics/exams/${examId}`);
  },

  getClassStatistics(classId) {
    return apiRequest(`/api/teacher/statistics/classes/${classId}`);
  },

  getSubjectStatistics(subjectId) {
    return apiRequest(`/api/teacher/statistics/subjects/${subjectId}`);
  }
};
