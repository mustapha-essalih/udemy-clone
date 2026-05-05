import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { CourseOverview } from '../api/courses';
import { listAllCourses, approveDraft, rejectDraft } from '../api/courses';

type StatusFilter = 'ALL' | 'DRAFT' | 'PENDING_REVIEW' | 'REJECTED' | 'PUBLISHED';

const STATUS_STYLES: Record<string, string> = {
  DRAFT: 'bg-zinc-700/60 text-zinc-300',
  PENDING_REVIEW: 'bg-yellow-900/60 text-yellow-300',
  REJECTED: 'bg-red-900/60 text-red-300',
  PUBLISHED: 'bg-green-900/60 text-green-300',
};

function isVideo(url: string) {
  return /\.(mp4|webm|ogg|mov|mkv)(\?|$)/i.test(url);
}

function isPdf(url: string) {
  return /\.pdf(\?|$)/i.test(url);
}

function fileLabel(url: string) {
  if (isVideo(url)) return 'VIDEO';
  if (isPdf(url)) return 'PDF';
  return 'FILE';
}

export default function ManagerDashboardPage() {
  const { user, logout, isManager, isAdmin } = useAuth();
  const navigate = useNavigate();
  const canReview = isManager || isAdmin;

  const [courses, setCourses] = useState<CourseOverview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<StatusFilter>('ALL');

  const [expandedCourses, setExpandedCourses] = useState<Set<string>>(new Set());
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set());
  const [previewLesson, setPreviewLesson] = useState<string | null>(null);

  const [modal, setModal] = useState<{ type: 'approve' | 'reject'; course: CourseOverview } | null>(null);
  const [feedback, setFeedback] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    listAllCourses()
      .then((res) => setCourses(res.data.data))
      .catch(() => setError('Failed to load courses. Make sure you are logged in with the correct role.'))
      .finally(() => setLoading(false));
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const toggle = (set: Set<string>, id: string): Set<string> => {
    const next = new Set(set);
    next.has(id) ? next.delete(id) : next.add(id);
    return next;
  };

  const handleAction = async () => {
    if (!modal) return;
    if (modal.type === 'reject' && !feedback.trim()) {
      setActionError('Feedback is required for rejection.');
      return;
    }
    setSubmitting(true);
    setActionError('');
    try {
      if (modal.type === 'approve') {
        await approveDraft(modal.course.id);
        setCourses((prev) => prev.filter((c) => c.id !== modal.course.id));
      } else {
        await rejectDraft(modal.course.id, feedback.trim());
        setCourses((prev) =>
          prev.map((c) =>
            c.id === modal.course.id
              ? { ...c, status: 'REJECTED', rejectionFeedback: feedback.trim() }
              : c
          )
        );
      }
      setModal(null);
      setFeedback('');
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string; status?: number } } }).response;
      if (res?.data?.status === 404) {
        setActionError('Draft not found — it may have already been processed. Refresh the page.');
      } else if (res?.data?.status === 422) {
        setActionError(res.data.message ?? 'Action not allowed in current status.');
      } else if (res?.data?.status === 409) {
        setActionError(res.data.message ?? 'Conflict: a course with this title already exists.');
      } else {
        setActionError('Action failed. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const filtered = filter === 'ALL' ? courses : courses.filter((c) => c.status === filter);
  const statusCounts = courses.reduce<Record<string, number>>((acc, c) => {
    acc[c.status] = (acc[c.status] ?? 0) + 1;
    return acc;
  }, {});

  const canActOn = (course: CourseOverview) =>
    canReview && course.source === 'REDIS' && course.status === 'PENDING_REVIEW';

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--bg)', color: 'var(--text)' }}>
      <header
        className="flex items-center gap-4 px-7 py-4 border-b"
        style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}
      >
        <h1 className="text-lg font-semibold">Course Review Dashboard</h1>
        <div className="ml-auto flex items-center gap-4">
          <span className="text-sm" style={{ color: 'var(--muted)' }}>
            {user?.email} <span className="role-badge">{user?.role}</span>
          </span>
          <button className="btn-secondary text-sm" onClick={() => navigate('/dashboard')}>
            Dashboard
          </button>
          <button className="btn-secondary text-sm" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      <main className="flex-1 px-7 py-7 w-full max-w-6xl">
        <div className="flex items-center justify-between mb-5 flex-wrap gap-3">
          <h2 className="text-base font-semibold">
            All Courses{' '}
            <span className="text-sm font-normal" style={{ color: 'var(--muted)' }}>
              ({courses.length})
            </span>
          </h2>
          <div className="flex gap-2 flex-wrap">
            {(['ALL', 'PENDING_REVIEW', 'DRAFT', 'REJECTED', 'PUBLISHED'] as StatusFilter[]).map((s) => (
              <button
                key={s}
                onClick={() => setFilter(s)}
                className="text-xs px-3 py-1 rounded-full border transition-colors"
                style={{
                  background: filter === s ? 'var(--primary)' : 'var(--surface)',
                  borderColor: filter === s ? 'var(--primary)' : 'var(--border)',
                  color: filter === s ? '#fff' : 'var(--muted)',
                }}
              >
                {s === 'ALL' ? 'All' : s.replace('_', ' ')}
                {s !== 'ALL' && statusCounts[s] != null ? ` (${statusCounts[s]})` : ''}
              </button>
            ))}
          </div>
        </div>

        {error && <div className="error-banner">{error}</div>}

        {loading ? (
          <div className="loading-msg">Loading courses…</div>
        ) : filtered.length === 0 ? (
          <div
            className="text-center py-16 text-sm rounded-xl border"
            style={{ color: 'var(--muted)', borderColor: 'var(--border)' }}
          >
            No courses for this filter.
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {filtered.map((course) => {
              const isCourseOpen = expandedCourses.has(course.id);
              return (
                <div
                  key={course.id}
                  className="rounded-xl border overflow-hidden"
                  style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}
                >
                  <div className="flex items-start gap-4 px-5 py-4">
                    <button
                      onClick={() => setExpandedCourses(toggle(expandedCourses, course.id))}
                      className="mt-0.5 flex-shrink-0 w-6 h-6 flex items-center justify-center rounded border text-[10px] transition-colors"
                      style={{ borderColor: 'var(--border)', background: 'var(--bg)', color: 'var(--muted)' }}
                      title={isCourseOpen ? 'Collapse' : 'Expand curriculum'}
                    >
                      {isCourseOpen ? '▲' : '▼'}
                    </button>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3 flex-wrap mb-1">
                        <span className="font-semibold text-sm truncate">{course.title}</span>
                        <span
                          className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded ${STATUS_STYLES[course.status] ?? 'bg-zinc-700/60 text-zinc-300'}`}
                        >
                          {course.status.replace('_', ' ')}
                        </span>
                        <span
                          className="text-[10px] px-2 py-0.5 rounded border"
                          style={{ borderColor: 'var(--border)', color: 'var(--muted)' }}
                        >
                          {course.source}
                        </span>
                      </div>

                      {course.subTitle && (
                        <p className="text-xs mb-1" style={{ color: 'var(--muted)' }}>
                          {course.subTitle}
                        </p>
                      )}

                      <div className="flex gap-3 flex-wrap">
                        {course.level && <span className="text-[11px]" style={{ color: 'var(--muted)' }}>{course.level}</span>}
                        {course.language && <span className="text-[11px]" style={{ color: 'var(--muted)' }}>{course.language}</span>}
                        {course.price != null && (
                          <span className="text-[11px]" style={{ color: 'var(--muted)' }}>
                            {course.isFree ? 'Free' : `$${course.price}`}
                          </span>
                        )}
                        {course.courseDurationMinutes != null && (
                          <span className="text-[11px]" style={{ color: 'var(--muted)' }}>
                            {course.courseDurationMinutes} min
                          </span>
                        )}
                        <span className="text-[11px]" style={{ color: 'var(--muted)' }}>
                          {course.sections.length} section{course.sections.length !== 1 ? 's' : ''}
                        </span>
                      </div>

                      {course.rejectionFeedback && (
                        <div
                          className="mt-2 text-xs px-3 py-2 rounded border"
                          style={{ background: '#3b1515', borderColor: 'var(--danger)', color: '#f9a0a0' }}
                        >
                          <span className="font-semibold">Rejection feedback: </span>
                          {course.rejectionFeedback}
                        </div>
                      )}
                    </div>

                    {canActOn(course) && (
                      <div className="flex gap-2 flex-shrink-0">
                        <button
                          className="text-xs px-3 py-1.5 rounded font-semibold"
                          style={{ background: '#0d2e1d', color: 'var(--success)' }}
                          onClick={() => { setModal({ type: 'approve', course }); setFeedback(''); setActionError(''); }}
                        >
                          Approve
                        </button>
                        <button
                          className="text-xs px-3 py-1.5 rounded font-semibold"
                          style={{ background: '#3b1515', color: '#f9a0a0' }}
                          onClick={() => { setModal({ type: 'reject', course }); setFeedback(''); setActionError(''); }}
                        >
                          Reject
                        </button>
                      </div>
                    )}
                  </div>

                  {isCourseOpen && (
                    <div
                      className="border-t px-5 py-4"
                      style={{ borderColor: 'var(--border)', background: 'var(--bg)' }}
                    >
                      {course.sections.length === 0 ? (
                        <p className="text-xs" style={{ color: 'var(--muted)' }}>No curriculum yet.</p>
                      ) : (
                        <div className="flex flex-col gap-2">
                          {course.sections.map((section) => {
                            const isSectionOpen = expandedSections.has(section.sectionId);
                            return (
                              <div
                                key={section.sectionId}
                                className="rounded-lg border overflow-hidden"
                                style={{ borderColor: 'var(--border)' }}
                              >
                                <button
                                  onClick={() => setExpandedSections(toggle(expandedSections, section.sectionId))}
                                  className="w-full flex items-center gap-3 px-4 py-2.5 text-left text-sm font-semibold"
                                  style={{ background: 'var(--surface)', color: 'var(--text)' }}
                                >
                                  <span className="text-[10px]" style={{ color: 'var(--muted)' }}>
                                    {isSectionOpen ? '▲' : '▼'}
                                  </span>
                                  <span>{section.title}</span>
                                  <span className="ml-auto text-[11px] font-normal" style={{ color: 'var(--muted)' }}>
                                    {section.lessons.length} lesson{section.lessons.length !== 1 ? 's' : ''}
                                  </span>
                                </button>

                                {isSectionOpen && (
                                  <ul className="divide-y" style={{ borderColor: 'var(--border)' }}>
                                    {section.lessons.map((lesson) => {
                                      const lessonKey = `${section.sectionId}:${lesson.lessonId}`;
                                      const isPreviewing = previewLesson === lessonKey;
                                      const hasVideo = !!lesson.videoUrl;
                                      const hasText = !!lesson.textUrl;

                                      return (
                                        <li key={lesson.lessonId} style={{ background: 'var(--bg)' }}>
                                          <div className="flex items-center gap-3 px-4 py-2.5">
                                            <span
                                              className="text-xs w-4 text-center flex-shrink-0"
                                              style={{ color: 'var(--muted)' }}
                                            >
                                              {lesson.lessonType === 'VIDEO' ? '▶' : '≡'}
                                            </span>
                                            <span className="text-sm flex-1 truncate">{lesson.title}</span>
                                            <span
                                              className="text-[10px] font-semibold uppercase flex-shrink-0"
                                              style={{ color: 'var(--muted)' }}
                                            >
                                              {lesson.lessonType}
                                            </span>
                                            {lesson.durationMinutes != null && (
                                              <span className="text-[11px] flex-shrink-0" style={{ color: 'var(--muted)' }}>
                                                {lesson.durationMinutes} min
                                              </span>
                                            )}
                                            {lesson.isPreview && <span className="preview-tag flex-shrink-0">Preview</span>}

                                            {(hasVideo || hasText) && (
                                              <button
                                                onClick={() => setPreviewLesson(isPreviewing ? null : lessonKey)}
                                                className="text-[11px] px-2 py-0.5 rounded border flex-shrink-0 transition-colors"
                                                style={{
                                                  borderColor: isPreviewing ? 'var(--primary)' : 'var(--border)',
                                                  color: isPreviewing ? 'var(--primary)' : 'var(--muted)',
                                                  background: isPreviewing ? 'rgba(108,99,255,0.1)' : 'transparent',
                                                }}
                                              >
                                                {isPreviewing ? 'Hide' : 'Preview'}
                                              </button>
                                            )}
                                          </div>

                                          {isPreviewing && (
                                            <div
                                              className="px-4 pb-4"
                                              style={{ borderTop: '1px solid var(--border)' }}
                                            >
                                              {hasVideo && lesson.videoUrl && (
                                                <div className="mt-3">
                                                  <p className="text-[11px] mb-1.5 font-semibold uppercase tracking-wider" style={{ color: 'var(--muted)' }}>
                                                    Video content
                                                  </p>
                                                  {isVideo(lesson.videoUrl) ? (
                                                    <video
                                                      controls
                                                      preload="metadata"
                                                      className="w-full rounded-lg"
                                                      style={{ maxHeight: 340, background: '#000' }}
                                                    >
                                                      <source src={lesson.videoUrl} />
                                                      <p className="text-xs" style={{ color: 'var(--muted)' }}>
                                                        Your browser does not support video playback.{' '}
                                                        <a href={lesson.videoUrl} target="_blank" rel="noreferrer" style={{ color: 'var(--primary)' }}>
                                                          Download
                                                        </a>
                                                      </p>
                                                    </video>
                                                  ) : (
                                                    <a
                                                      href={lesson.videoUrl}
                                                      target="_blank"
                                                      rel="noreferrer"
                                                      className="text-xs inline-flex items-center gap-1.5 px-3 py-1.5 rounded border"
                                                      style={{ borderColor: 'var(--border)', color: 'var(--primary)' }}
                                                    >
                                                      {fileLabel(lesson.videoUrl)} ↗
                                                    </a>
                                                  )}
                                                </div>
                                              )}

                                              {hasText && lesson.textUrl && (
                                                <div className="mt-3">
                                                  <p className="text-[11px] mb-1.5 font-semibold uppercase tracking-wider" style={{ color: 'var(--muted)' }}>
                                                    {lesson.lessonType === 'ARTICLE' ? 'Article' : 'Text content'}
                                                  </p>
                                                  {isPdf(lesson.textUrl) ? (
                                                    <div>
                                                      <iframe
                                                        src={lesson.textUrl}
                                                        title={lesson.title}
                                                        className="w-full rounded-lg border"
                                                        style={{ height: 420, borderColor: 'var(--border)' }}
                                                      />
                                                      <a
                                                        href={lesson.textUrl}
                                                        target="_blank"
                                                        rel="noreferrer"
                                                        className="text-xs mt-1 inline-block"
                                                        style={{ color: 'var(--muted)' }}
                                                      >
                                                        Open PDF in new tab ↗
                                                      </a>
                                                    </div>
                                                  ) : isVideo(lesson.textUrl) ? (
                                                    <video
                                                      controls
                                                      preload="metadata"
                                                      className="w-full rounded-lg"
                                                      style={{ maxHeight: 340, background: '#000' }}
                                                    >
                                                      <source src={lesson.textUrl} />
                                                    </video>
                                                  ) : (
                                                    <a
                                                      href={lesson.textUrl}
                                                      target="_blank"
                                                      rel="noreferrer"
                                                      className="text-xs inline-flex items-center gap-1.5 px-3 py-1.5 rounded border"
                                                      style={{ borderColor: 'var(--border)', color: 'var(--primary)' }}
                                                    >
                                                      {fileLabel(lesson.textUrl)} — Open file ↗
                                                    </a>
                                                  )}
                                                </div>
                                              )}
                                            </div>
                                          )}
                                        </li>
                                      );
                                    })}
                                  </ul>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </main>

      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0"
            style={{ background: 'rgba(0,0,0,0.7)' }}
            onClick={() => !submitting && setModal(null)}
          />
          <div
            className="relative rounded-xl border p-6 w-full max-w-md z-10"
            style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}
          >
            <h3 className="text-base font-semibold mb-1">
              {modal.type === 'approve' ? 'Approve Course' : 'Reject Course'}
            </h3>
            <p className="text-sm mb-4" style={{ color: 'var(--muted)' }}>
              {modal.course.title}
            </p>

            {modal.type === 'approve' ? (
              <p className="text-sm mb-4" style={{ color: 'var(--muted)' }}>
                This will publish the course and make it available to students.
              </p>
            ) : (
              <div className="mb-4">
                <label className="block text-xs mb-1 font-medium" style={{ color: 'var(--muted)' }}>
                  Feedback for the instructor <span style={{ color: 'var(--danger)' }}>*</span>
                </label>
                <textarea
                  rows={4}
                  placeholder="Explain why this course is being rejected…"
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                  disabled={submitting}
                  className="resize-none"
                />
              </div>
            )}

            {actionError && <div className="error-banner">{actionError}</div>}

            <div className="flex gap-3 justify-end">
              <button className="btn-secondary text-sm" onClick={() => setModal(null)} disabled={submitting}>
                Cancel
              </button>
              <button
                className="text-sm px-4 py-2 rounded font-semibold disabled:opacity-50"
                style={
                  modal.type === 'approve'
                    ? { background: '#0d2e1d', color: 'var(--success)' }
                    : { background: '#3b1515', color: '#f9a0a0' }
                }
                onClick={handleAction}
                disabled={submitting}
              >
                {submitting ? 'Processing…' : modal.type === 'approve' ? 'Confirm Approve' : 'Confirm Reject'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
