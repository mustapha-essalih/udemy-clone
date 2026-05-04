import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { CourseResponse } from '../api/courses';
import { listCourses } from '../api/courses';

export default function DashboardPage() {
  const { user, logout, isInstructor } = useAuth();
  const navigate = useNavigate();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isInstructor || !user) {
      setLoading(false);
      return;
    }
    listCourses(user.userId)
      .then((res) => setCourses(res.data.data))
      .catch(() => setError('Failed to load courses'))
      .finally(() => setLoading(false));
  }, [isInstructor, user]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="page">
      <header className="page-header">
        <h1>Dashboard</h1>
        <div className="header-right">
          <span className="user-info">
            {user?.email} <span className="role-badge">{user?.role}</span>
          </span>
          <button className="btn-secondary" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      <main className="page-main">
        {isInstructor ? (
          <section>
            <div className="section-header">
              <h2>My Courses</h2>
              <Link to="/courses/new" className="btn-primary">
                + New Course
              </Link>
            </div>

            {error && <div className="error-banner">{error}</div>}

            {loading ? (
              <p className="muted">Loading…</p>
            ) : courses.length === 0 ? (
              <p className="muted">No courses yet. Create your first one.</p>
            ) : (
              <div className="course-grid">
                {courses.map((c) => (
                  <div key={c.courseId} className="course-card">
                    <div className="course-card-status">{c.status}</div>
                    <h3>{c.title}</h3>
                    {c.subTitle && <p className="muted">{c.subTitle}</p>}
                    <div className="course-card-meta">
                      {c.level && <span>{c.level}</span>}
                      {c.language && <span>{c.language}</span>}
                      {c.price != null && <span>{c.isFree ? 'Free' : `$${c.price}`}</span>}
                    </div>
                    <div className="course-card-actions">
                      <Link
                        to={`/courses/${c.courseId}`}
                        className="btn-secondary"
                        style={{ fontSize: 12, padding: '4px 10px' }}
                      >
                        Edit
                      </Link>
                      <Link
                        to={`/learn/${c.courseId}`}
                        className="btn-primary"
                        style={{ fontSize: 12, padding: '4px 10px' }}
                      >
                        View
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        ) : (
          <div className="welcome-box">
            <h2>Welcome, {user?.email}</h2>
            <p className="muted">You're signed in as a <strong>{user?.role}</strong>.</p>
          </div>
        )}
      </main>
    </div>
  );
}
