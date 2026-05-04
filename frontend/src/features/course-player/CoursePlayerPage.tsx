import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getCourse } from '../../api/courses';
import type { CourseResponse, LessonResponse } from '../../api/courses';
import { useAuth } from '../../context/AuthContext';
import CurriculumSidebar from './CurriculumSidebar';
import LessonView from './LessonView';

export default function CoursePlayerPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeLesson, setActiveLesson] = useState<LessonResponse | null>(null);

  useEffect(() => {
    if (!courseId) return;
    getCourse(courseId)
      .then((res) => {
        const data = res.data.data;
        setCourse(data);
        const allLessons = data.sections.flatMap((s) => s.lessons);
        const first = allLessons.find((l) => l.videoUrl) ?? allLessons[0] ?? null;
        setActiveLesson(first);
      })
      .catch(() => setError('Failed to load course'))
      .finally(() => setLoading(false));
  }, [courseId]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (loading) {
    return (
      <div className="player-page">
        <p className="muted loading-msg">Loading…</p>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="player-page">
        <div style={{ padding: 28 }}>
          <div className="error-banner">{error || 'Course not found'}</div>
        </div>
      </div>
    );
  }

  const totalLessons = course.sections.reduce((n, s) => n + s.lessons.length, 0);

  return (
    <div className="player-page">
      <header className="page-header">
        <Link to="/dashboard" className="back-link">← Dashboard</Link>
        <div style={{ flex: 1, minWidth: 0, marginLeft: 8 }}>
          <h1 style={{ fontSize: 17, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {course.title}
          </h1>
          <div style={{ display: 'flex', gap: 10, marginTop: 3, flexWrap: 'wrap' }}>
            <span className="muted small">{course.level}</span>
            <span className="muted small">·</span>
            <span className="muted small">{course.sections.length} sections · {totalLessons} lessons</span>
            {course.courseDurationMinutes > 0 && (
              <>
                <span className="muted small">·</span>
                <span className="muted small">{course.courseDurationMinutes} min</span>
              </>
            )}
          </div>
        </div>
        <div className="header-right">
          <span className="user-info">
            {user?.email} <span className="role-badge">{user?.role}</span>
          </span>
          <button className="btn-secondary" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <div className="player-body">
        <CurriculumSidebar
          sections={course.sections}
          activeLesson={activeLesson}
          onSelect={setActiveLesson}
        />
        <LessonView lesson={activeLesson} />
      </div>
    </div>
  );
}
