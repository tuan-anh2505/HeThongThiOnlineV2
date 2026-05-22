import { clearStoredAuth, getStoredToken } from "./authStorage.js";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

let unauthorizedHandler = null;

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

export async function apiRequest(path, options = {}) {
  const { skipAuth = false, headers, body, ...fetchOptions } = options;
  const requestHeaders = new Headers(headers);

  if (!requestHeaders.has("Content-Type") && body !== undefined && !(body instanceof FormData)) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (!skipAuth) {
    const token = getStoredToken();
    if (token) {
      requestHeaders.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchOptions,
    headers: requestHeaders,
    body: body instanceof FormData || typeof body === "string" ? body : JSON.stringify(body)
  });

  const responseBody = await parseResponseBody(response);

  if (response.status === 401) {
    clearStoredAuth();
    unauthorizedHandler?.();
  }

  if (!response.ok) {
    throw new ApiError(resolveErrorMessage(responseBody, response.statusText), response.status, responseBody);
  }

  return responseBody;
}

async function parseResponseBody(response) {
  const contentType = response.headers.get("Content-Type") ?? "";
  if (response.status === 204) {
    return null;
  }
  if (contentType.includes("application/json")) {
    return response.json();
  }
  const text = await response.text();
  return text || null;
}

function resolveErrorMessage(body, fallback) {
  if (!body) {
    return fallback || "Yêu cầu không thành công";
  }
  if (typeof body === "string") {
    return body;
  }
  return body.message || body.detail || body.error || fallback || "Yêu cầu không thành công";
}
