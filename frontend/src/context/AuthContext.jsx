import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { setUnauthorizedHandler } from "../services/apiClient.js";
import { fetchCurrentUser, getHomePathForRole, login as loginRequest } from "../services/authService.js";
import { clearStoredAuth, getStoredAuth, saveStoredAuth } from "../services/authStorage.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [auth, setAuth] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);

  const logout = useCallback(() => {
    clearStoredAuth();
    setAuth(null);
    setAuthLoading(false);
    navigate("/login", { replace: true });
  }, [navigate]);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  useEffect(() => {
    let cancelled = false;

    async function verifyStoredSession() {
      const storedAuth = getStoredAuth();
      if (!storedAuth?.token) {
        clearStoredAuth();
        setAuth(null);
        setAuthLoading(false);
        return;
      }

      setAuthLoading(true);
      try {
        const currentUser = await fetchCurrentUser();
        if (cancelled) {
          return;
        }

        const nextAuth = {
          token: storedAuth.token,
          tokenType: storedAuth.tokenType ?? "Bearer",
          expiresIn: storedAuth.expiresIn,
          user: currentUser
        };
        saveStoredAuth(nextAuth);
        setAuth(nextAuth);
      } catch {
        if (!cancelled) {
          clearStoredAuth();
          setAuth(null);
        }
      } finally {
        if (!cancelled) {
          setAuthLoading(false);
        }
      }
    }

    verifyStoredSession();

    return () => {
      cancelled = true;
    };
  }, []);

  const signIn = useCallback(
    async ({ username, password }) => {
      const response = await loginRequest(username, password);
      const nextAuth = {
        token: response.token,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
        user: response.user
      };
      saveStoredAuth(nextAuth);
      setAuth(nextAuth);
      setAuthLoading(false);
      navigate(getHomePathForRole(response.user.role), { replace: true });
      return response.user;
    },
    [navigate]
  );

  const value = useMemo(
    () => ({
      token: auth?.token ?? null,
      user: auth?.user ?? null,
      isAuthenticated: Boolean(auth?.token && auth?.user),
      authLoading,
      signIn,
      logout
    }),
    [auth, authLoading, logout, signIn]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
