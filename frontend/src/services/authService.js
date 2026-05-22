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

export async function forgotPassword(email) {
  return apiRequest("/api/auth/forgot-password", {
    method: "POST",
    skipAuth: true,
    body: { email }
  });
}

export async function resetPassword(email, otp, newPassword, confirmPassword) {
  return apiRequest("/api/auth/reset-password", {
    method: "POST",
    skipAuth: true,
    body: {
      email,
      otp,
      newPassword,
      confirmPassword
    }
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
