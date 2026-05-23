import { apiRequest } from "./apiClient.js";

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
    return value;
  }

  return value === null || value === undefined ? undefined : value;
}

function requestWithBody(path, method, payload) {
  return apiRequest(path, {
    method,
    body: cleanPayload(payload) ?? {}
  });
}

export const studentService = {
  getSubjects() {
    return apiRequest("/api/student/subjects");
  },

  getExams() {
    return apiRequest("/api/student/exams");
  },

  getExamDetail(examId) {
    return apiRequest(`/api/student/exams/${examId}`);
  },

  startExam(examId, examPassword) {
    return requestWithBody(`/api/student/exams/${examId}/start`, "POST", { examPassword });
  },

  getAttempt(attemptId) {
    return apiRequest(`/api/student/attempts/${attemptId}`);
  },

  saveAnswer(attemptId, questionId, payload) {
    return requestWithBody(`/api/student/attempts/${attemptId}/answers/${questionId}`, "PUT", payload);
  },

  submitAttempt(attemptId) {
    return apiRequest(`/api/student/attempts/${attemptId}/submit`, { method: "POST" });
  },

  autoSubmitAttempt(attemptId) {
    return apiRequest(`/api/student/attempts/${attemptId}/auto-submit`, { method: "POST" });
  },

  getAttemptStatus(attemptId) {
    return apiRequest(`/api/student/attempts/${attemptId}/status`);
  },

  getAttemptResult(attemptId) {
    return apiRequest(`/api/student/attempts/${attemptId}/result`);
  },

  getAttemptReview(attemptId) {
    return apiRequest(`/api/student/attempts/${attemptId}/review`);
  },

  getResults() {
    return apiRequest("/api/student/results");
  }
};
