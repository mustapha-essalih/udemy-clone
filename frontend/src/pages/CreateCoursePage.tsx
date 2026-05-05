import { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { createCourse, updateDraftVideoUrl, updateDraftTextUrl } from '../api/courses';
import type { DraftSectionResponse, DraftLessonResponse } from '../api/courses';
import { startUpload, uploadChunk, completeUpload, pauseUpload, resumeUpload, cancelUpload, CHUNK_SIZE } from '../api/content';
import type { BasicInfo, DraftSection, UploadTarget, UploadState, ValidationErrors } from './create-course/types';
import { LEVEL_MAP, LESSON_TYPE_MAP, validateBasicInfo } from './create-course/types';
import StepBasicInfo from './create-course/StepBasicInfo';
import StepCurriculum from './create-course/StepCurriculum';
import StepUpload from './create-course/StepUpload';
import StepAttach from './create-course/StepAttach';
import StepReview from './create-course/StepReview';
import './create-course/wizard.css';

const STEPS = [
  { num: '01', label: 'Setup',     title: 'Basic Info' },
  { num: '02', label: 'Structure', title: 'Curriculum' },
  { num: '03', label: 'Content',   title: 'Upload' },
  { num: '04', label: 'Linking',   title: 'Attach' },
  { num: '05', label: 'Final',     title: 'Review & Submit' },
];

const UPLOAD_CONCURRENCY = 3;
let draftCounter = 0;
const newDraftId = () => `d${++draftCounter}`;

// ─── Icons ───
const CheckIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 12l5 5L20 7"/>
  </svg>
);
const ChevRIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M9 6l6 6-6 6"/>
  </svg>
);
const ChevLIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M15 6l-6 6 6 6"/>
  </svg>
);
const EyeIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z"/>
  </svg>
);
const RocketIcon = () => (
  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 19c1-4 3-6 7-7 1-4 4-8 9-9-1 5-5 8-9 9-1 4-3 6-7 7zM9 15l-2 6 6-2"/>
  </svg>
);

// Nav icons
const DashboardIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" stroke="none">
    <rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/>
    <rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>
  </svg>
);
const BookIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M4 4.5A1.5 1.5 0 0 1 5.5 3H19a1 1 0 0 1 1 1v15a1 1 0 0 1-1 1H6.5A2.5 2.5 0 0 1 4 17.5zM4 17.5A2.5 2.5 0 0 1 6.5 15H20"/>
  </svg>
);
const PlusIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <path d="M12 5v14M5 12h14"/>
  </svg>
);
const GlobeIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>
  </svg>
);
const DollarIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 3v18M16 7H10a3 3 0 0 0 0 6h4a3 3 0 0 1 0 6H8"/>
  </svg>
);
const CogIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="3"/>
    <path d="M19 12c0 .8-.1 1.5-.3 2.2l2 1.5-2 3.4-2.4-.7c-1 .8-2.2 1.4-3.5 1.7L12 22h-4l-.8-1.9a8 8 0 0 1-3.5-1.7l-2.4.7-2-3.4 2-1.5A8 8 0 0 1 1 12c0-.8.1-1.5.3-2.2l-2-1.5 2-3.4 2.4.7C4.7 4.8 5.9 4.2 7.2 3.9L8 2h4l.8 1.9c1.3.3 2.5.9 3.5 1.7l2.4-.7 2 3.4-2 1.5c.2.7.3 1.5.3 2.2z"/>
  </svg>
);

// ─── Sidebar ───
function Sidebar({ email }: { email: string }) {
  const initial = email ? email[0].toUpperCase() : '?';
  const nav = [
    { id: 'dash',    label: 'Dashboard',     Icon: DashboardIcon },
    { id: 'courses', label: 'My Courses',    Icon: BookIcon, count: 4 },
    { id: 'create',  label: 'Create Course', Icon: PlusIcon, active: true },
  ];
  const sub = [
    { id: 'students', label: 'Students', Icon: GlobeIcon },
    { id: 'earnings', label: 'Earnings', Icon: DollarIcon },
    { id: 'settings', label: 'Settings', Icon: CogIcon },
  ];
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">C</div>
        <div>
          <div className="brand-name">Course Studio</div>
          <div className="brand-sub">Instructor</div>
        </div>
      </div>

      <div className="nav-label">Teach</div>
      {nav.map(({ id, label, Icon, count, active }: any) => (
        <div key={id} className={`nav-item${active ? ' active' : ''}`}>
          <span className="nav-icon"><Icon /></span>
          <span style={{ flex: 1 }}>{label}</span>
          {count != null && <span style={{ fontFamily: 'var(--mono)', fontSize: 11, color: 'var(--ink-4)' }}>{count}</span>}
        </div>
      ))}

      <div className="nav-label" style={{ marginTop: 8 }}>Insights</div>
      {sub.map(({ id, label, Icon }: any) => (
        <div key={id} className="nav-item">
          <span className="nav-icon"><Icon /></span>
          <span>{label}</span>
        </div>
      ))}

      <div className="nav-spacer" />

      <div className="profile-card">
        <div className="avatar">{initial}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="profile-name" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{email}</div>
          <div className="profile-role">Instructor</div>
        </div>
      </div>
    </aside>
  );
}

// ─── Submitted modal ───
function SubmittedScreen({ onReset, courseId }: { onReset: () => void; courseId: string }) {
  const navigate = useNavigate();
  return (
    <div className="modal-backdrop">
      <div className="modal">
        <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--green-soft)', color: 'var(--green)', display: 'grid', placeItems: 'center', margin: '0 auto 16px' }}>
          <RocketIcon />
        </div>
        <h3>Course submitted!</h3>
        <p>Your course is now in review. We'll email you within 2–5 business days with the result.</p>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
          <button className="btn" onClick={onReset}>Edit again</button>
          <button className="btn primary" onClick={() => navigate(courseId ? `/courses/${courseId}` : '/dashboard')}>
            View course
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main ───
export default function CreateCoursePage() {
  const { user } = useAuth();

  const [step, setStep] = useState(0);
  const [submitted, setSubmitted] = useState(false);
  const [saveState, setSaveState] = useState<'saving' | 'saved'>('saved');
  const [toast, setToast] = useState<string | null>(null);
  const [createError, setCreateError] = useState('');
  const [creating, setCreating] = useState(false);

  const [basicInfo, setBasicInfo] = useState<BasicInfo>({
    title: '', subtitle: '', description: '',
    category: '', subcategory: '', level: 'beginner',
    language: 'English', free: false, price: '',
  });

  const [draftSections, setDraftSections] = useState<DraftSection[]>([
    { draftId: newDraftId(), title: '', lessons: [{ draftId: newDraftId(), title: '', type: 'video' }] },
  ]);

  const [courseId, setCourseId] = useState('');
  const [targets, setTargets] = useState<UploadTarget[]>([]);
  const [uploads, setUploads] = useState<Record<string, UploadState>>({});
  const [activeLessonId, setActiveLessonId] = useState<string | null>(null);
  const pauseFlags = useRef<Record<string, boolean>>({});
  const speedSamples = useRef<Record<string, number[]>>({});

  const errors: ValidationErrors = useMemo(() => validateBasicInfo(basicInfo), [basicInfo]);

  useEffect(() => {
    setSaveState('saving');
    const t = setTimeout(() => setSaveState('saved'), 800);
    return () => clearTimeout(t);
  }, [basicInfo, draftSections, step]);

  const set = (k: keyof BasicInfo, v: string | boolean) =>
    setBasicInfo((d) => ({ ...d, [k]: v }));

  const patchUpload = useCallback((lessonId: string, patch: Partial<UploadState>) =>
    setUploads((prev) => ({ ...prev, [lessonId]: { ...prev[lessonId], ...patch } })), []);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2800);
  };

  const canAdvance = useMemo(() => {
    if (step === 0) return Object.keys(errors).length === 0;
    if (step === 1) {
      return draftSections.length > 0 &&
        draftSections.every((s) => s.title.trim() && s.lessons.length > 0 && s.lessons.every((l) => l.title.trim()));
    }
    return true;
  }, [step, errors, draftSections]);

  const goTo = (i: number) => {
    const next = Math.max(0, Math.min(4, i));
    // Pause any active uploads when leaving the upload step so they don't
    // keep writing to state after the user has navigated away.
    if (step === 2 && next !== 2) {
      Object.entries(uploads).forEach(([lessonId, state]) => {
        if (state.status === 'uploading' && state.sessionId) {
          pauseFlags.current[state.sessionId] = true;
          pauseUpload(state.sessionId).catch(() => {});
          patchUpload(lessonId, { status: 'paused' });
        }
      });
    }
    setStep(next);
  };

  // ─── Course creation ───
  const handleCreateCourse = async () => {
    setCreating(true);
    setCreateError('');
    try {
      const res = await createCourse({
        title: basicInfo.title,
        subTitle: basicInfo.subtitle,
        description: basicInfo.description,
        price: basicInfo.free ? 0 : parseFloat(basicInfo.price) || 0,
        isFree: basicInfo.free,
        language: basicInfo.language,
        courseDurationMinutes: 1,
        level: LEVEL_MAP[basicInfo.level] || 'BEGINNER',
        sections: draftSections.map((s) => ({
          title: s.title,
          lessons: s.lessons.map((l) => ({
            title: l.title,
            lessonType: LESSON_TYPE_MAP[l.type] as 'VIDEO' | 'TEXT' | 'ARTICLE',
          })),
        })),
      });

      const course = res.data.data;
      setCourseId(course.draftId);

      const targetList: UploadTarget[] = [];
      course.sections.forEach((sec: DraftSectionResponse, si: number) => {
        sec.lessons.forEach((lesson: DraftLessonResponse, li: number) => {
          targetList.push({
            lessonId: lesson.lessonId,
            sectionId: sec.sectionId,
            courseId: course.draftId,
            lessonTitle: lesson.title,
            sectionTitle: sec.title,
            type: draftSections[si]?.lessons[li]?.type ?? 'video',
          });
        });
      });

      const initUploads: Record<string, UploadState> = {};
      targetList.forEach((t) => {
        initUploads[t.lessonId] = { file: null, sessionId: null, status: 'idle', percent: 0, speed: 0, errorMsg: '', linkedUrl: null };
      });

      setTargets(targetList);
      setUploads(initUploads);
      setActiveLessonId(targetList[0]?.lessonId ?? null);
      goTo(2);
      showToast('Course created — now upload your content');
    } catch (err: any) {
      setCreateError(err.response?.data?.message ?? 'Failed to create course. Check all fields and try again.');
    } finally {
      setCreating(false);
    }
  };

  // ─── Chunked upload ───
  const sendChunks = async (lessonId: string, file: File, sessionId: string, indices: number[]) => {
    let cursor = 0;
    const worker = async () => {
      while (cursor < indices.length) {
        if (pauseFlags.current[sessionId]) return;
        const i = indices[cursor++];
        const start = i * CHUNK_SIZE;
        const before = Date.now();
        const res = await uploadChunk(sessionId, i, file.slice(start, Math.min(start + CHUNK_SIZE, file.size)));
        const elapsed = (Date.now() - before) / 1000 || 0.001;
        const chunkBytes = Math.min(CHUNK_SIZE, file.size - start);
        const instantSpeed = chunkBytes / elapsed;
        const samples = speedSamples.current[sessionId] = [...(speedSamples.current[sessionId] ?? []), instantSpeed].slice(-5);
        const avgSpeed = samples.reduce((a, b) => a + b, 0) / samples.length;
        patchUpload(lessonId, { percent: res.data.data.percentComplete, speed: avgSpeed });
      }
    };
    await Promise.all(Array.from({ length: Math.min(UPLOAD_CONCURRENCY, indices.length) }, worker));
  };

  const handleFileSelect = (lessonId: string, file: File) => {
    patchUpload(lessonId, { file, status: 'idle', percent: 0, errorMsg: '', speed: 0, sessionId: null, linkedUrl: null });
  };

  const handleStart = useCallback(async (lessonId: string) => {
    const state = uploads[lessonId];
    if (!state?.file) return;
    const file = state.file;
    const target = targets.find((t) => t.lessonId === lessonId)!;
    patchUpload(lessonId, { status: 'starting', errorMsg: '' });
    let sessionId = '';
    try {
      const startRes = await startUpload({
        ownerId: user!.userId,
        courseId: target.courseId,
        sectionId: target.sectionId,
        lessonId: target.lessonId,
        courseName: basicInfo.title,
        sectionName: target.sectionTitle,
        lessonTitle: target.lessonTitle,
        fileName: file.name,
        mimeType: file.type || 'application/octet-stream',
        totalSize: file.size,
        chunkSize: CHUNK_SIZE,
      });
      ({ sessionId } = startRes.data.data);
      const { totalChunks } = startRes.data.data;
      pauseFlags.current[sessionId] = false;
      speedSamples.current[sessionId] = [];
      patchUpload(lessonId, { sessionId, status: 'uploading' });
      await sendChunks(lessonId, file, sessionId, Array.from({ length: totalChunks }, (_, i) => i));
      if (pauseFlags.current[sessionId]) return;
      const completeRes = await completeUpload(sessionId);
      const { url } = completeRes.data.data;
      if (target.type === 'video') {
        await updateDraftVideoUrl(target.courseId, target.sectionId, target.lessonId, { videoUrl: url });
      } else {
        await updateDraftTextUrl(target.courseId, target.sectionId, target.lessonId, { contentUrl: url });
      }
      patchUpload(lessonId, { status: 'done', percent: 100, linkedUrl: url });
    } catch (err: any) {
      if (!pauseFlags.current[sessionId]) {
        patchUpload(lessonId, { status: 'error', errorMsg: err.response?.data?.message ?? 'Upload failed' });
      }
    }
  }, [uploads, targets, basicInfo.title, user, patchUpload]);

  const handlePause = useCallback(async (lessonId: string) => {
    const state = uploads[lessonId];
    if (!state?.sessionId) return;
    pauseFlags.current[state.sessionId] = true;
    try { await pauseUpload(state.sessionId); } catch { /* best-effort */ }
    patchUpload(lessonId, { status: 'paused' });
  }, [uploads, patchUpload]);

  const handleResume = useCallback(async (lessonId: string) => {
    const state = uploads[lessonId];
    if (!state?.sessionId || !state.file) return;
    const { sessionId } = state;
    const target = targets.find((t) => t.lessonId === lessonId)!;
    try {
      const res = await resumeUpload(sessionId);
      const { missingChunks } = res.data.data;
      pauseFlags.current[sessionId] = false;
      patchUpload(lessonId, { status: 'uploading' });
      await sendChunks(lessonId, state.file, sessionId, missingChunks);
      if (pauseFlags.current[sessionId]) return;
      const completeRes = await completeUpload(sessionId);
      const { url } = completeRes.data.data;
      if (target.type === 'video') {
        await updateDraftVideoUrl(target.courseId, target.sectionId, target.lessonId, { videoUrl: url });
      } else {
        await updateDraftTextUrl(target.courseId, target.sectionId, target.lessonId, { contentUrl: url });
      }
      patchUpload(lessonId, { status: 'done', percent: 100, linkedUrl: url });
    } catch {
      patchUpload(lessonId, { status: 'error', errorMsg: 'Resume failed' });
    }
  }, [uploads, targets, patchUpload]);

  const handleCancel = useCallback(async (lessonId: string) => {
    const state = uploads[lessonId];
    if (state?.sessionId) {
      pauseFlags.current[state.sessionId] = true;
      try { await cancelUpload(state.sessionId); } catch { /* best-effort */ }
    }
    patchUpload(lessonId, { file: null, sessionId: null, status: 'idle', percent: 0, speed: 0, errorMsg: '', linkedUrl: null });
  }, [uploads, patchUpload]);

  const handleReupload = useCallback(async (lessonId: string) => {
    const state = uploads[lessonId];
    if (state?.sessionId) {
      pauseFlags.current[state.sessionId] = true;
      try { await cancelUpload(state.sessionId); } catch { /* best-effort */ }
    }
    patchUpload(lessonId, { file: null, sessionId: null, status: 'idle', percent: 0, speed: 0, errorMsg: '', linkedUrl: null });
  }, [uploads, patchUpload]);

  // ─── Step navigation ───
  const handleNext = async () => {
    if (step === 1) {
      if (courseId) { goTo(2); return; }
      await handleCreateCourse();
    } else {
      goTo(step + 1);
      showToast('Progress saved');
    }
  };

  const pageHeaders = [
    { eyebrow: 'Step 01', title: 'The basics',            sub: "Cover the essentials so students know what they'll learn." },
    { eyebrow: 'Step 02', title: 'Build your curriculum', sub: 'Group your lessons into sections. Click any title to rename it inline.' },
    { eyebrow: 'Step 03', title: 'Upload your content',   sub: 'Drop video or PDF files into each lesson. Pause, resume, or cancel anytime.' },
    { eyebrow: 'Step 04', title: 'Attached content',      sub: 'Confirm every uploaded file is linked to the right lesson.' },
    { eyebrow: 'Step 05', title: 'Review & submit',       sub: 'Last look before your course heads to our review team.' },
  ];
  const ph = pageHeaders[step];

  return (
    <div className="course-wizard">
      <div className="app">
        <Sidebar email={user?.email ?? ''} />

        <div className="main">
          {/* Topbar */}
          <div className="topbar">
            <div className="crumbs">
              <Link to="/dashboard">Instructor</Link>
              <span className="sep">/</span>
              <span>Create course</span>
              <span className="sep">/</span>
              <strong>{basicInfo.title || 'Untitled course'}</strong>
            </div>
            <div className="topbar-right">
              <span className="save-pill">
                <span className={`save-dot${saveState === 'saving' ? ' saving' : ''}`} />
                {saveState === 'saving' ? 'Autosaving…' : 'Draft saved · just now'}
              </span>
              <button type="button" className="btn">
                <EyeIcon /> Preview
              </button>
            </div>
          </div>

          {/* Stepper */}
          <div className="stepper">
            {STEPS.map((s, i) => (
              <div
                key={s.num}
                className={`step${i === step ? ' current' : i < step ? ' done' : ''}`}
                onClick={() => goTo(i)}
              >
                <span className="step-num">
                  {i < step ? <CheckIcon /> : s.num}
                </span>
                <span className="step-meta">
                  <span className="step-label">{s.label}</span>
                  <span className="step-title">{s.title}</span>
                </span>
              </div>
            ))}
          </div>

          {/* Canvas */}
          <div className="canvas">
            <div className="canvas-inner">
              {/* Page header */}
              <div className="eyebrow">{ph.eyebrow}</div>
              <h1 className="page-h">{ph.title}</h1>
              <p className="page-sub">{ph.sub}</p>

              {createError && (
                <div className="error-banner">{createError}</div>
              )}

              {step === 0 && <StepBasicInfo data={basicInfo} set={set} errors={errors} />}
              {step === 1 && <StepCurriculum sections={draftSections} setSections={setDraftSections} />}
              {step === 2 && (
                <StepUpload
                  targets={targets}
                  uploads={uploads}
                  activeLessonId={activeLessonId}
                  setActiveLessonId={setActiveLessonId}
                  onFileSelect={handleFileSelect}
                  onStart={handleStart}
                  onPause={handlePause}
                  onResume={handleResume}
                  onCancel={handleCancel}
                  onReupload={handleReupload}
                />
              )}
              {step === 3 && (
                <StepAttach
                  targets={targets}
                  uploads={uploads}
                  setActiveLessonId={setActiveLessonId}
                  goTo={goTo}
                />
              )}
              {step === 4 && (
                <StepReview
                  data={basicInfo}
                  sections={draftSections}
                  targets={targets}
                  uploads={uploads}
                  onSubmit={() => setSubmitted(true)}
                />
              )}

              {/* Navigation */}
              <div className="step-nav">
                <button
                  type="button"
                  className="btn ghost"
                  disabled={step === 0}
                  onClick={() => goTo(step - 1)}
                >
                  <ChevLIcon /> Back
                </button>
                <span className="step-nav-info">
                  Step <strong style={{ color: 'var(--ink)' }}>{step + 1}</strong> of {STEPS.length} · {STEPS[step].title}
                </span>
                {step < 4 ? (
                  <button
                    type="button"
                    className="btn primary"
                    disabled={!canAdvance || creating}
                    onClick={handleNext}
                  >
                    {creating ? 'Creating course…' : <><span>Continue</span> <ChevRIcon /></>}
                  </button>
                ) : (
                  <span style={{ width: 100 }} />
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Toast */}
      {toast && (
        <div className="toast">
          <CheckIcon /> {toast}
        </div>
      )}

      {/* Submitted modal */}
      {submitted && (
        <SubmittedScreen onReset={() => { setSubmitted(false); setStep(0); }} courseId={courseId} />
      )}
    </div>
  );
}
