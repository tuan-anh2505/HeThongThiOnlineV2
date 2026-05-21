const AUTH_STORAGE_KEY = "online_exam_auth";

export function getStoredAuth() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw);
    if (!parsed?.token || !parsed?.user) {
      clearStoredAuth();
      return null;
    }
    return parsed;
  } catch {
    clearStoredAuth();
    return null;
  }
}

export function saveStoredAuth(auth) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearStoredAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function getStoredToken() {
  return getStoredAuth()?.token ?? null;
}
