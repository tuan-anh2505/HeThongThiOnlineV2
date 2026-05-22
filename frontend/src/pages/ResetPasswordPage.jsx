import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { resetPassword } from "../services/authService.js";

export function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: location.state?.email || "",
    otp: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const validateForm = () => {
    if (!form.email.trim()) {
      return "Vui lòng nhập email";
    }
    if (!form.otp.trim()) {
      return "Vui lòng nhập mã xác thực";
    }
    if (!form.newPassword.trim()) {
      return "Vui lòng nhập mật khẩu mới";
    }
    if (!form.confirmPassword.trim()) {
      return "Vui lòng xác nhận mật khẩu mới";
    }
    if (form.newPassword !== form.confirmPassword) {
      return "Mật khẩu xác nhận không khớp";
    }
    return "";
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const validationMessage = validateForm();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setLoading(true);
    try {
      const response = await resetPassword(
        form.email.trim(),
        form.otp.trim(),
        form.newPassword,
        form.confirmPassword
      );
      setSuccess(response?.message || "Đặt lại mật khẩu thành công");
      window.setTimeout(() => navigate("/login", { replace: true }), 1200);
    } catch (err) {
      setError(err.message || "Không thể đặt lại mật khẩu. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="reset-password-title">
        <div className="login-heading">
          <p className="eyebrow">Hệ thống thi online</p>
          <h1 id="reset-password-title">Đặt lại mật khẩu</h1>
          <p>Nhập mã xác thực đã nhận qua email và mật khẩu mới.</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              autoComplete="email"
              required
            />
          </label>

          <label>
            Mã xác thực
            <input
              name="otp"
              value={form.otp}
              onChange={handleChange}
              inputMode="numeric"
              autoComplete="one-time-code"
              required
            />
          </label>

          <label>
            Mật khẩu mới
            <input
              name="newPassword"
              type="password"
              value={form.newPassword}
              onChange={handleChange}
              autoComplete="new-password"
              required
            />
          </label>

          <label>
            Xác nhận mật khẩu mới
            <input
              name="confirmPassword"
              type="password"
              value={form.confirmPassword}
              onChange={handleChange}
              autoComplete="new-password"
              required
            />
          </label>

          {success ? <p className="success-message">{success}</p> : null}
          {error ? <p className="error-message">{error}</p> : null}

          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? "Đang đặt lại..." : "Đặt lại mật khẩu"}
          </button>
        </form>

        <div className="form-links">
          <Link to="/forgot-password">Gửi lại mã xác thực</Link>
          <Link to="/login">Quay lại đăng nhập</Link>
        </div>
      </section>
    </main>
  );
}
