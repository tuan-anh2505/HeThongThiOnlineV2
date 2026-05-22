import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { changePassword, fetchProfile } from "../services/profileService.js";

export function ProfilePage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [profileError, setProfileError] = useState("");
  const [profileLoading, setProfileLoading] = useState(true);
  const [form, setForm] = useState({
    oldPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let mounted = true;

    async function loadProfile() {
      setProfileLoading(true);
      setProfileError("");
      try {
        const response = await fetchProfile();
        if (mounted) {
          setProfile(response);
        }
      } catch (err) {
        if (mounted) {
          setProfileError(err.message || "Không thể tải hồ sơ người dùng");
        }
      } finally {
        if (mounted) {
          setProfileLoading(false);
        }
      }
    }

    loadProfile();
    return () => {
      mounted = false;
    };
  }, []);

  const account = profile?.account || user;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const validateForm = () => {
    if (!form.oldPassword.trim()) {
      return "Vui lòng nhập mật khẩu hiện tại";
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
      const response = await changePassword(form.oldPassword, form.newPassword, form.confirmPassword);
      setSuccess(response?.message || "Đổi mật khẩu thành công");
      setForm({
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
      });
    } catch (err) {
      setError(err.message || "Không thể đổi mật khẩu. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="profile-page" aria-labelledby="profile-title">
      <div className="section-heading">
        <p className="eyebrow">Tài khoản</p>
        <h2 id="profile-title">Hồ sơ cá nhân</h2>
        <p>Quản lý thông tin tài khoản và đổi mật khẩu đăng nhập.</p>
      </div>

      <div className="profile-layout">
        <article className="profile-card">
          <h3>Thông tin tài khoản</h3>
          {profileLoading ? <p className="muted-text">Đang tải hồ sơ...</p> : null}
          {profileError ? <p className="error-message">{profileError}</p> : null}
          {!profileLoading && !profileError ? (
            <dl className="profile-details">
              <div>
                <dt>Họ tên</dt>
                <dd>{account?.fullName || "Chưa cập nhật"}</dd>
              </div>
              <div>
                <dt>Tên đăng nhập</dt>
                <dd>{account?.username}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{account?.email || "Chưa cập nhật"}</dd>
              </div>
              <div>
                <dt>Vai trò</dt>
                <dd>{account?.role}</dd>
              </div>
              <div>
                <dt>Trạng thái</dt>
                <dd>{account?.status}</dd>
              </div>
            </dl>
          ) : null}
        </article>

        <article className="profile-card">
          <h3>Đổi mật khẩu</h3>
          <form className="login-form" onSubmit={handleSubmit}>
            <label>
              Mật khẩu hiện tại
              <input
                name="oldPassword"
                type="password"
                value={form.oldPassword}
                onChange={handleChange}
                autoComplete="current-password"
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
              {loading ? "Đang đổi mật khẩu..." : "Đổi mật khẩu"}
            </button>
          </form>
        </article>
      </div>
    </section>
  );
}
