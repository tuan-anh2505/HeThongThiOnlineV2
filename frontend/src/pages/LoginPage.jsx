import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { getHomePathForRole } from "../services/authService.js";

export function LoginPage() {
  const navigate = useNavigate();
  const { isAuthenticated, user, signIn } = useAuth();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated && user) {
      navigate(getHomePathForRole(user.role), { replace: true });
    }
  }, [isAuthenticated, navigate, user]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await signIn(form);
    } catch (err) {
      setError(err.message || "Không thể đăng nhập. Vui lòng kiểm tra lại thông tin.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-heading">
          <p className="eyebrow">Hệ thống thi online</p>
          <h1 id="login-title">Đăng nhập</h1>
          <p>Nhập tài khoản được cấp để truy cập hệ thống.</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Tên đăng nhập
            <input
              name="username"
              value={form.username}
              onChange={handleChange}
              autoComplete="username"
              required
            />
          </label>

          <label>
            Mật khẩu
            <input
              name="password"
              type="password"
              value={form.password}
              onChange={handleChange}
              autoComplete="current-password"
              required
            />
          </label>

          {error ? <p className="error-message">{error}</p> : null}

          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? "Đang đăng nhập..." : "Đăng nhập"}
          </button>
        </form>

        <div className="form-links">
          <Link to="/forgot-password">Quên mật khẩu?</Link>
        </div>
      </section>
    </main>
  );
}
