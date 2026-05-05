import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import CreateCoursePage from './pages/CreateCoursePage';
import CourseDetailPage from './pages/CourseDetailPage';
import ManagerDashboardPage from './pages/ManagerDashboardPage';
import CoursePlayerPage from './features/course-player/CoursePlayerPage';
import SearchPage from './features/search/SearchPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/courses/new"
            element={
              <ProtectedRoute requireInstructor>
                <CreateCoursePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/courses/:id"
            element={
              <ProtectedRoute requireInstructor>
                <CourseDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/learn/:courseId"
            element={
              <ProtectedRoute>
                <CoursePlayerPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/manager"
            element={
              <ProtectedRoute>
                <ManagerDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route path="/search" element={<SearchPage />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
