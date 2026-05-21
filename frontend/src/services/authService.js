import { apiRequest } from "./apiClient.js";

export async function login(username, password) {
  return apiRequest("/api/auth/login", {
    method: "POST",
    skipAuth: true,
    body: { username, password }
  });
}

export async function fetchCurrentUser() {
  return apiRequest("/api/auth/me", {
    method: "GET"
  });
}

export function getHomePathForRole(role) {
  switch (role) {
    case "ADMIN":
      return "/admin/dashboard";
    case "TEACHER":
      return "/teacher/dashboard";
    case "STUDENT":
      return "/student/dashboard";
    default:
      return "/forbidden";
  }
}
