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
    return value.map(cleanPayload).filter((item) => item !== undefined);
  }

  if (value && typeof value === "object") {
    const cleaned = Object.entries(value).reduce((result, [key, currentValue]) => {
      const nextValue = cleanPayload(currentValue);
      if (nextValue !== undefined) {
        result[key] = nextValue;
      }
      return result;
    }, {});

    return cleaned;
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

export const adminService = {
  searchAccounts(filters) {
    return apiRequest(`/api/admin/accounts${buildQuery(filters)}`);
  },

  createAccount(payload) {
    return requestWithBody("/api/admin/accounts", "POST", payload);
  },

  updateAccount(id, payload) {
    return requestWithBody(`/api/admin/accounts/${id}`, "PUT", payload);
  },

  lockAccount(id) {
    return apiRequest(`/api/admin/accounts/${id}/lock`, { method: "PATCH" });
  },

  unlockAccount(id) {
    return apiRequest(`/api/admin/accounts/${id}/unlock`, { method: "PATCH" });
  },

  deleteAccount(id) {
    return apiRequest(`/api/admin/accounts/${id}`, { method: "DELETE" });
  },

  searchStudents(filters) {
    return apiRequest(`/api/admin/students${buildQuery(filters)}`);
  },

  searchTeachers(filters) {
    return apiRequest(`/api/admin/teachers${buildQuery(filters)}`);
  },

  searchClasses(filters) {
    return apiRequest(`/api/classes${buildQuery(filters)}`);
  },

  createClass(payload) {
    return requestWithBody("/api/classes", "POST", payload);
  },

  updateClass(id, payload) {
    return requestWithBody(`/api/classes/${id}`, "PUT", payload);
  },

  deleteClass(id) {
    return apiRequest(`/api/classes/${id}`, { method: "DELETE" });
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

  searchSubjects(filters) {
    return apiRequest(`/api/subjects${buildQuery(filters)}`);
  },

  createSubject(payload) {
    return requestWithBody("/api/subjects", "POST", payload);
  },

  updateSubject(id, payload) {
    return requestWithBody(`/api/subjects/${id}`, "PUT", payload);
  },

  deleteSubject(id) {
    return apiRequest(`/api/subjects/${id}`, { method: "DELETE" });
  },

  searchAssignments(filters) {
    return apiRequest(`/api/class-subject-teachers${buildQuery(filters)}`);
  },

  createAssignment(payload) {
    return requestWithBody("/api/class-subject-teachers", "POST", payload);
  },

  updateAssignment(id, payload) {
    return requestWithBody(`/api/class-subject-teachers/${id}`, "PUT", payload);
  },

  deleteAssignment(id) {
    return apiRequest(`/api/class-subject-teachers/${id}`, { method: "DELETE" });
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

  searchExams(filters) {
    return apiRequest(`/api/exams${buildQuery(filters)}`);
  },

  searchLogs(filters) {
    return apiRequest(`/api/admin/logs${buildQuery(filters)}`);
  },

  getExamStatistics(examId) {
    return apiRequest(`/api/admin/statistics/exams/${examId}`);
  },

  getClassStatistics(classId) {
    return apiRequest(`/api/admin/statistics/classes/${classId}`);
  },

  getSubjectStatistics(subjectId) {
    return apiRequest(`/api/admin/statistics/subjects/${subjectId}`);
  }
};
