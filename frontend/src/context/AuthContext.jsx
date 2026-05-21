import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { setUnauthorizedHandler } from "../services/apiClient.js";
import { getHomePathForRole, login as loginRequest } from "../services/authService.js";
import { clearStoredAuth, getStoredAuth, saveStoredAuth } from "../services/authStorage.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [auth, setAuth] = useState(() => getStoredAuth());

  const logout = useCallback(() => {
    clearStoredAuth();
    setAuth(null);
    navigate("/login", { replace: true });
  }, [navigate]);

  useEffect(() => {
    setUnauthorizedHandler(() => logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

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
      signIn,
      logout
    }),
    [auth, logout, signIn]
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
