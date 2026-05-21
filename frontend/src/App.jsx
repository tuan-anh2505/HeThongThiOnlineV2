import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./components/ProtectedRoute.jsx";
import { RoleLayout } from "./layouts/RoleLayout.jsx";
import { AdminDashboard } from "./pages/AdminDashboard.jsx";
import { ForbiddenPage } from "./pages/ForbiddenPage.jsx";
import { LoginPage } from "./pages/LoginPage.jsx";
import { NotFoundPage } from "./pages/NotFoundPage.jsx";
import { StudentDashboard } from "./pages/StudentDashboard.jsx";
import { TeacherDashboard } from "./pages/TeacherDashboard.jsx";
import { getHomePathForRole } from "./services/authService.js";
import { useAuth } from "./context/AuthContext.jsx";

function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? getHomePathForRole(user.role) : "/login"} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forbidden" element={<ForbiddenPage />} />

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
