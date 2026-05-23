import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./components/ProtectedRoute.jsx";
import { RoleLayout } from "./layouts/RoleLayout.jsx";
import { AdminDashboard } from "./pages/AdminDashboard.jsx";
import { AdminAccountsPage } from "./pages/admin/AdminAccountsPage.jsx";
import { AdminAssignmentsPage } from "./pages/admin/AdminAssignmentsPage.jsx";
import { AdminClassesPage } from "./pages/admin/AdminClassesPage.jsx";
import { AdminClassStudentsPage } from "./pages/admin/AdminClassStudentsPage.jsx";
import { AdminLogsPage } from "./pages/admin/AdminLogsPage.jsx";
import { AdminQuestionBanksPage } from "./pages/admin/AdminQuestionBanksPage.jsx";
import { AdminStatisticsPage } from "./pages/admin/AdminStatisticsPage.jsx";
import { AdminSubjectsPage } from "./pages/admin/AdminSubjectsPage.jsx";
import { ForbiddenPage } from "./pages/ForbiddenPage.jsx";
import { ForgotPasswordPage } from "./pages/ForgotPasswordPage.jsx";
import { LoginPage } from "./pages/LoginPage.jsx";
import { NotFoundPage } from "./pages/NotFoundPage.jsx";
import { ProfilePage } from "./pages/ProfilePage.jsx";
import { ResetPasswordPage } from "./pages/ResetPasswordPage.jsx";
import { StudentDashboard } from "./pages/StudentDashboard.jsx";
import { TeacherDashboard } from "./pages/TeacherDashboard.jsx";
import { TeacherAttemptDetailPage } from "./pages/teacher/TeacherAttemptDetailPage.jsx";
import { TeacherAttemptsPage } from "./pages/teacher/TeacherAttemptsPage.jsx";
import { TeacherClassesPage } from "./pages/teacher/TeacherClassesPage.jsx";
import { TeacherClassStudentsPage } from "./pages/teacher/TeacherClassStudentsPage.jsx";
import { TeacherExamQuestionsPage } from "./pages/teacher/TeacherExamQuestionsPage.jsx";
import { TeacherExamsPage } from "./pages/teacher/TeacherExamsPage.jsx";
import { TeacherQuestionBanksPage } from "./pages/teacher/TeacherQuestionBanksPage.jsx";
import { TeacherQuestionsPage } from "./pages/teacher/TeacherQuestionsPage.jsx";
import { TeacherSessionsPage } from "./pages/teacher/TeacherSessionsPage.jsx";
import { TeacherStatisticsPage } from "./pages/teacher/TeacherStatisticsPage.jsx";
import { useAuth } from "./context/AuthContext.jsx";
import { getHomePathForRole } from "./services/authService.js";

const ALL_ROLES = ["ADMIN", "TEACHER", "STUDENT"];

function HomeRedirect() {
  const { authLoading, user } = useAuth();
  if (authLoading) {
    return (
      <main className="state-page">
        <h1>Đang kiểm tra đăng nhập</h1>
        <p>Vui lòng chờ trong giây lát.</p>
      </main>
    );
  }
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
        <Route path="accounts" element={<AdminAccountsPage />} />
        <Route path="classes" element={<AdminClassesPage />} />
        <Route path="class-students" element={<AdminClassStudentsPage />} />
        <Route path="subjects" element={<AdminSubjectsPage />} />
        <Route path="assignments" element={<AdminAssignmentsPage />} />
        <Route path="question-banks" element={<AdminQuestionBanksPage />} />
        <Route path="logs" element={<AdminLogsPage />} />
        <Route path="statistics" element={<AdminStatisticsPage />} />
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
        <Route path="classes" element={<TeacherClassesPage />} />
        <Route path="classes/:classId/students" element={<TeacherClassStudentsPage />} />
        <Route path="class-students" element={<TeacherClassStudentsPage />} />
        <Route path="question-banks" element={<TeacherQuestionBanksPage />} />
        <Route path="question-banks/:bankId/questions" element={<TeacherQuestionsPage />} />
        <Route path="questions" element={<TeacherQuestionsPage />} />
        <Route path="exams" element={<TeacherExamsPage />} />
        <Route path="exams/:examId/questions" element={<TeacherExamQuestionsPage />} />
        <Route path="exams/:examId/sessions" element={<TeacherSessionsPage />} />
        <Route path="sessions" element={<TeacherExamsPage />} />
        <Route path="exams/:examId/attempts" element={<TeacherAttemptsPage />} />
        <Route path="attempts" element={<TeacherAttemptsPage />} />
        <Route path="attempts/:attemptId" element={<TeacherAttemptDetailPage />} />
        <Route path="statistics" element={<TeacherStatisticsPage />} />
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
