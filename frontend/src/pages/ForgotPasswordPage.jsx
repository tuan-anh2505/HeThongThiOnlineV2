import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { forgotPassword } from "../services/authService.js";

const DEFAULT_SUCCESS_MESSAGE = "Nếu email tồn tại trong hệ thống, mã xác thực sẽ được gửi đến email đó";

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!email.trim()) {
      setError("Vui lòng nhập email");
      return;
    }

    setLoading(true);
    try {
      const response = await forgotPassword(email.trim());
      setMessage(response?.message || DEFAULT_SUCCESS_MESSAGE);
    } catch (err) {
      setError(err.message || "Không thể gửi mã xác thực. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="forgot-password-title">
        <div className="login-heading">
          <p className="eyebrow">Hệ thống thi online</p>
          <h1 id="forgot-password-title">Quên mật khẩu</h1>
          <p>Nhập email tài khoản để nhận mã xác thực đặt lại mật khẩu.</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              name="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              required
            />
          </label>

          {message ? <p className="success-message">{message}</p> : null}
          {error ? <p className="error-message">{error}</p> : null}

          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? "Đang gửi..." : "Gửi mã xác thực"}
          </button>
        </form>

        <div className="form-links">
          {message ? (
            <button
              className="link-button"
              type="button"
              onClick={() => navigate("/reset-password", { state: { email: email.trim() } })}
            >
              Nhập mã xác thực
            </button>
          ) : null}
          <Link to="/login">Quay lại đăng nhập</Link>
        </div>
      </section>
    </main>
  );
}
