import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./components/ProtectedRoute.jsx";
import { RoleLayout } from "./layouts/RoleLayout.jsx";
import { AdminDashboard } from "./pages/AdminDashboard.jsx";
import { ForbiddenPage } from "./pages/ForbiddenPage.jsx";
import { ForgotPasswordPage } from "./pages/ForgotPasswordPage.jsx";
import { LoginPage } from "./pages/LoginPage.jsx";
import { NotFoundPage } from "./pages/NotFoundPage.jsx";
import { ProfilePage } from "./pages/ProfilePage.jsx";
import { ResetPasswordPage } from "./pages/ResetPasswordPage.jsx";
import { StudentDashboard } from "./pages/StudentDashboard.jsx";
import { TeacherDashboard } from "./pages/TeacherDashboard.jsx";
import { useAuth } from "./context/AuthContext.jsx";
import { getHomePathForRole } from "./services/authService.js";

const ALL_ROLES = ["ADMIN", "TEACHER", "STUDENT"];

function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? getHomePathForRole(user.role) : "/login"} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/forbidden" element={<ForbiddenPage />} />

      <Route
        path="/profile"
        element={
          <ProtectedRoute allowedRoles={ALL_ROLES}>
            <RoleLayout title="Hồ sơ cá nhân" />
          </ProtectedRoute>
        }
      >
        <Route index element={<ProfilePage />} />
      </Route>

      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <RoleLayout roleName="Admin" />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<AdminDashboard />} />
      </Route>

      <Route
        path="/teacher"
        element={
          <ProtectedRoute allowedRoles={["TEACHER"]}>
            <RoleLayout roleName="Giảng viên" />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<TeacherDashboard />} />
      </Route>

      <Route
        path="/student"
        element={
          <ProtectedRoute allowedRoles={["STUDENT"]}>
            <RoleLayout roleName="Sinh viên" />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<StudentDashboard />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
