import { useEffect, useState, useRef } from 'react';

const UPLOAD_CONCURRENCY = 3;
import { useParams, Link } from 'react-router-dom';
import type { CourseResponse, SectionResponse, LessonResponse } from '../api/courses';
import { getCourse, updateVideoUrl } from '../api/courses';
import {
  startUpload, uploadChunk, completeUpload,
  pauseUpload, resumeUpload, cancelUpload, CHUNK_SIZE,
} from '../api/content';
import { useAuth } from '../context/AuthContext';

interface UploadState {
  file: File | null;
  sessionId: string | null;
  status: 'idle' | 'starting' | 'uploading' | 'paused' | 'done' | 'error';
  percent: number;
  errorMsg: string;
}

export default function CourseDetailPage() {
  const { id: courseId } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [uploads, setUploads] = useState<Record<string, UploadState>>({});
  const pauseFlags = useRef<Record<string, boolean>>({});

  useEffect(() => {
    if (!courseId) return;
    getCourse(courseId)
      .then((res) => setCourse(res.data.data))
      .catch(() => setError('Failed to load course'))
      .finally(() => setLoading(false));
  }, [courseId]);

  const patchUpload = (lessonId: string, patch: Partial<UploadState>) =>
    setUploads((prev) => ({ ...prev, [lessonId]: { ...prev[lessonId], ...patch } }));

  const getState = (lessonId: string): UploadState =>
    uploads[lessonId] ?? { file: null, sessionId: null, status: 'idle', percent: 0, errorMsg: '' };

  const sendChunks = async (lessonId: string, file: File, sessionId: string, indices: number[]) => {
    let cursor = 0;
    const worker = async () => {
      while (cursor < indices.length) {
        if (pauseFlags.current[sessionId]) return;
        const i = indices[cursor++];
        const start = i * CHUNK_SIZE;
        const res = await uploadChunk(sessionId, i, file.slice(start, Math.min(start + CHUNK_SIZE, file.size)));
        patchUpload(lessonId, { percent: res.data.data.percentComplete });
      }
    };
    await Promise.all(Array.from({ length: Math.min(UPLOAD_CONCURRENCY, indices.length) }, worker));
  };

  const handleStart = async (section: SectionResponse, lesson: LessonResponse) => {
    const state = getState(lesson.lessonId);
    if (!state.file || !courseId) return;
    const file = state.file;
    patchUpload(lesson.lessonId, { status: 'starting', errorMsg: '' });
    let sessionId = '';
    try {
      const startRes = await startUpload({
        ownerId: user!.userId,
        courseId,
        sectionId: section.sectionId,
        lessonId: lesson.lessonId,
        courseName: course!.title,
        sectionName: section.title,
        lessonTitle: lesson.title,
        fileName: file.name,
        mimeType: file.type || 'application/octet-stream',
        totalSize: file.size,
        chunkSize: CHUNK_SIZE,
      });
      ({ sessionId } = startRes.data.data);
      const { totalChunks } = startRes.data.data;
      pauseFlags.current[sessionId] = false;
      patchUpload(lesson.lessonId, { sessionId, status: 'uploading' });
      await sendChunks(lesson.lessonId, file, sessionId, Array.from({ length: totalChunks }, (_, i) => i));
      if (pauseFlags.current[sessionId]) return;
      const completeRes = await completeUpload(sessionId);
      await updateVideoUrl(courseId, section.sectionId, lesson.lessonId, { videoUrl: completeRes.data.data.url });
      const refreshed = await getCourse(courseId);
      setCourse(refreshed.data.data);
      patchUpload(lesson.lessonId, { status: 'done', percent: 100 });
    } catch (err: any) {
      if (!pauseFlags.current[sessionId]) {
        patchUpload(lesson.lessonId, { status: 'error', errorMsg: err.response?.data?.message ?? 'Upload failed' });
      }
    }
  };

  const handleResume = async (section: SectionResponse, lesson: LessonResponse) => {
    const state = getState(lesson.lessonId);
    if (!state.sessionId || !state.file) return;
    const sessionId = state.sessionId;
    try {
      const res = await resumeUpload(sessionId);
      const { missingChunks } = res.data.data;
      pauseFlags.current[sessionId] = false;
      patchUpload(lesson.lessonId, { status: 'uploading' });
      await sendChunks(lesson.lessonId, state.file, sessionId, missingChunks);
      if (pauseFlags.current[sessionId]) return;
      const completeRes = await completeUpload(sessionId);
      await updateVideoUrl(courseId!, section.sectionId, lesson.lessonId, { videoUrl: completeRes.data.data.url });
      const refreshed = await getCourse(courseId!);
      setCourse(refreshed.data.data);
      patchUpload(lesson.lessonId, { status: 'done', percent: 100 });
    } catch {
      patchUpload(lesson.lessonId, { status: 'error', errorMsg: 'Resume failed' });
    }
  };

  const handlePause = async (lesson: LessonResponse) => {
    const state = getState(lesson.lessonId);
    if (!state.sessionId) return;
    pauseFlags.current[state.sessionId] = true;
    try { await pauseUpload(state.sessionId); } catch {  }
    patchUpload(lesson.lessonId, { status: 'paused' });
  };

  const handleCancel = async (lesson: LessonResponse) => {
    const state = getState(lesson.lessonId);
    if (state.sessionId) {
      pauseFlags.current[state.sessionId] = true;
      try { await cancelUpload(state.sessionId); } catch {  }
    }
    patchUpload(lesson.lessonId, { file: null, sessionId: null, status: 'idle', percent: 0, errorMsg: '' });
  };

  if (loading) return <div className="page"><p className="muted loading-msg">Loading…</p></div>;
  if (error || !course) return <div className="page"><div className="error-banner">{error || 'Course not found'}</div></div>;

  return (
    <div className="page">
      <header className="page-header">
        <Link to="/dashboard" className="back-link">← Dashboard</Link>
        <div>
          <h1>{course.title}</h1>
          <span className="role-badge">{course.status}</span>
        </div>
      </header>

      <main className="page-main">
        <div className="card" style={{ marginBottom: 24 }}>
          <div className="course-meta-grid">
            <div>
              <span className="meta-label">Level</span>
              <span>{course.level}</span>
            </div>
            <div>
              <span className="meta-label">Language</span>
              <span>{course.language}</span>
            </div>
            <div>
              <span className="meta-label">Duration</span>
              <span>{course.courseDurationMinutes} min</span>
            </div>
            <div>
              <span className="meta-label">Price</span>
              <span>{course.isFree ? 'Free' : `$${course.price}`}</span>
            </div>
            {course.rating > 0 && (
              <div>
                <span className="meta-label">Rating</span>
                <span>{course.rating.toFixed(1)} / 5</span>
              </div>
            )}
          </div>
          {course.subTitle && <p style={{ marginTop: 12, color: 'var(--muted)' }}>{course.subTitle}</p>}
          {course.description && <p style={{ marginTop: 8 }}>{course.description}</p>}
        </div>

        {course.sections.length === 0 && (
          <p className="muted">No sections yet.</p>
        )}
        {course.sections.map((section) => (
          <section key={section.sectionId} className="card">
            <h2 className="section-title">{section.title}</h2>
            {section.lessons.length === 0 ? (
              <p className="muted small">No lessons</p>
            ) : (
              <ul className="lesson-list">
                {section.lessons.map((lesson) => {
                  const state = getState(lesson.lessonId);
                  const isActive = state.status === 'uploading' || state.status === 'starting';
                  return (
                    <li key={lesson.lessonId} className="lesson-item">
                      <div className="lesson-header">
                        <span className="lesson-name">{lesson.title}</span>
                        <span className="muted small">{lesson.lessonType}</span>
                        {lesson.videoUrl && <span className="media-tag">video attached</span>}
                      </div>

                      {lesson.lessonType === 'VIDEO' && !lesson.videoUrl && (
                        <>
                          {state.status === 'done' ? (
                            <span className="success-text">✓ Uploaded</span>
                          ) : (
                            <>
                              {(state.status === 'idle' || state.status === 'error') && (
                                <input
                                  type="file"
                                  accept="video/*"
                                  onChange={(e) =>
                                    patchUpload(lesson.lessonId, {
                                      file: e.target.files?.[0] ?? null,
                                      status: 'idle',
                                      percent: 0,
                                      errorMsg: '',
                                    })
                                  }
                                />
                              )}
                              {state.errorMsg && (
                                <div className="error-text" style={{ marginTop: 6 }}>{state.errorMsg}</div>
                              )}
                              {(state.status === 'uploading' || state.status === 'paused') && (
                                <div className="progress-bar">
                                  <div className="progress-fill" style={{ width: `${state.percent}%` }} />
                                  <span className="progress-label">{state.percent.toFixed(1)}%</span>
                                </div>
                              )}
                              <div className="upload-row" style={{ marginTop: 8 }}>
                                {(state.status === 'idle' || state.status === 'error') && (
                                  <button
                                    className="btn-primary small"
                                    disabled={!state.file}
                                    onClick={() => handleStart(section, lesson)}
                                  >
                                    Upload
                                  </button>
                                )}
                                {isActive && (
                                  <button className="btn-secondary small" onClick={() => handlePause(lesson)}>
                                    Pause
                                  </button>
                                )}
                                {state.status === 'paused' && (
                                  <button className="btn-primary small" onClick={() => handleResume(section, lesson)}>
                                    Resume
                                  </button>
                                )}
                                {(isActive || state.status === 'paused') && (
                                  <button className="btn-secondary small" onClick={() => handleCancel(lesson)}>
                                    Cancel
                                  </button>
                                )}
                              </div>
                            </>
                          )}
                        </>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </section>
        ))}
      </main>
    </div>
  );
}
