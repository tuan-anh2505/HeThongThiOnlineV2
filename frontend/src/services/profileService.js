import { apiRequest } from "./apiClient.js";

export async function fetchProfile() {
  return apiRequest("/api/profile/me", {
    method: "GET"
  });
}

export async function changePassword(oldPassword, newPassword, confirmPassword) {
  return apiRequest("/api/profile/change-password", {
    method: "PUT",
    body: {
      oldPassword,
      newPassword,
      confirmPassword
    }
  });
}
