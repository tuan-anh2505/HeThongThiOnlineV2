const AUTH_STORAGE_KEY = "online_exam_auth";

function clearLegacyLocalStorage() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function getStoredAuth() {
  clearLegacyLocalStorage();

  const raw = sessionStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw);
    if (!parsed?.token) {
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
  clearLegacyLocalStorage();
  sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearStoredAuth() {
  sessionStorage.removeItem(AUTH_STORAGE_KEY);
  clearLegacyLocalStorage();
}

export function getStoredToken() {
  return getStoredAuth()?.token ?? null;
}
