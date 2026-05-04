import type { BasicInfo, DraftSection, UploadTarget, UploadState } from './types';

const CheckIcon = () => (
  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 12l5 5L20 7"/>
  </svg>
);
const LockIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>
  </svg>
);
const RocketIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 19c1-4 3-6 7-7 1-4 4-8 9-9-1 5-5 8-9 9-1 4-3 6-7 7zM9 15l-2 6 6-2"/>
  </svg>
);
const VideoIcon = ({ size = 12 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 7a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM16 9.5l5-3v11l-5-3z"/>
  </svg>
);
const PdfIcon = ({ size = 12 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M7 3h7l5 5v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM14 3v5h5"/>
  </svg>
);
const ArticleIcon = ({ size = 12 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M4 6h16M4 10h16M4 14h10M4 18h8"/>
  </svg>
);

interface Props {
  data: BasicInfo;
  sections: DraftSection[];
  targets: UploadTarget[];
  uploads: Record<string, UploadState>;
  onSubmit: () => void;
}

export default function StepReview({ data, sections, targets, uploads, onSubmit }: Props) {
  const totalLessons = sections.reduce((a, s) => a + s.lessons.length, 0);
  const completed = targets.filter(t => uploads[t.lessonId]?.status === 'done').length;

  const checks = [
    { ok: data.title.length >= 5,                      label: 'Title is at least 5 characters' },
    { ok: data.description.length >= 200,              label: 'Description is at least 200 characters', warn: data.description.length >= 50 && data.description.length < 200 },
    { ok: !!data.category && !!data.subcategory,       label: 'Category & subcategory selected' },
    { ok: !!data.level,                                label: 'Difficulty level set' },
    { ok: data.free || !!data.price,                   label: data.free ? 'Course is free' : 'Pricing tier set' },
    { ok: sections.length >= 1,                        label: 'At least one section' },
    { ok: totalLessons >= 1,                           label: 'At least one lesson' },
    { ok: targets.length > 0 && completed === targets.length, label: 'All lessons have content uploaded', warn: completed > 0 && completed < targets.length },
  ];
  const allReady = checks.every((c) => c.ok);

  return (
    <div className="step-content">
      <div className="review-grid">
        {/* Left: course summary */}
        <div className="card review-summary">
          <div className="card-h">
            <h3>Course summary</h3>
            <span className="count mono">DRAFT · v1</span>
          </div>

          {/* Hero */}
          <div style={{ padding: '20px 0', borderBottom: '1px solid var(--hairline)', marginBottom: 4 }}>
            <div className="eyebrow" style={{ marginBottom: 6 }}>
              {(data.category || 'uncategorized').toUpperCase()} · {(data.level || '').toUpperCase()}
            </div>
            <h2 style={{ fontFamily: 'var(--display)', fontSize: 36, fontWeight: 400, lineHeight: 1.05, letterSpacing: '-0.015em', margin: '0 0 10px', color: 'var(--ink)' }}>
              {data.title || 'Untitled course'}
            </h2>
            {data.subtitle && <p className="muted" style={{ fontSize: 15 }}>{data.subtitle}</p>}
          </div>

          {/* Metadata rows */}
          {[
            { k: 'Description', v: data.description || '—', pre: true },
            { k: 'Category',    v: `${data.category || '—'} · ${data.subcategory || '—'}` },
            { k: 'Level',       v: `${data.level || '—'} · ${data.language}`, cap: true },
            { k: 'Pricing',     v: data.free ? 'Free' : `$${data.price || '—'}` },
            { k: 'Curriculum',  v: `${sections.length} sections · ${totalLessons} lessons` },
          ].map((row) => (
            <div key={row.k} className="review-row">
              <span className="k">{row.k}</span>
              <span className={`v${row.pre ? '' : ''}${row.cap ? ' capitalize' : ''}`} style={row.pre ? { whiteSpace: 'pre-wrap' } : {}}>
                {row.v}
              </span>
            </div>
          ))}

          {/* Curriculum preview */}
          <div className="curriculum-preview">
            {sections.map((s, si) => (
              <div key={s.draftId} className="cp-section">
                <div className="cp-section-h">
                  <span className="cp-section-title">
                    <span className="mono muted" style={{ marginRight: 8 }}>{String(si + 1).padStart(2, '0')}</span>
                    {s.title}
                  </span>
                  <span className="cp-section-meta">{s.lessons.length} lessons</span>
                </div>
                {s.lessons.map((l) => {
                  const t = targets.find(t => t.lessonTitle === l.title && t.sectionTitle === s.title);
                  const isDone = t ? uploads[t.lessonId]?.status === 'done' : false;
                  return (
                    <div key={l.draftId} className="cp-lesson">
                      {l.type === 'video' ? <VideoIcon /> : l.type === 'pdf' ? <PdfIcon /> : <ArticleIcon />}
                      <span style={{ flex: 1 }}>{l.title}</span>
                      {isDone ? (
                        <span className="mono muted" style={{ fontSize: 11 }}>done</span>
                      ) : (
                        <span className="pill red" style={{ fontSize: 10 }}>missing file</span>
                      )}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>

        {/* Right: checklist + submit */}
        <div className="review-side" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="card checklist-card">
            <h4>Submission checklist</h4>
            {checks.map((c, i) => (
              <div key={i} className="check-item">
                <span className={`check-circle${c.ok ? ' done' : c.warn ? ' warn' : ''}`}>
                  {c.ok && <CheckIcon />}
                </span>
                <span className={c.ok ? '' : 'muted'}>{c.label}</span>
              </div>
            ))}

            <div className="divider" />

            <button
              type="button"
              className="btn accent"
              disabled={!allReady}
              onClick={onSubmit}
              style={{ width: '100%', justifyContent: 'center' }}
            >
              <RocketIcon /> Submit for review
            </button>
            <p className="muted" style={{ fontSize: 11.5, marginTop: 10, lineHeight: 1.55 }}>
              Reviews typically take <strong style={{ color: 'var(--ink-2)' }}>2–5 business days</strong>. You'll get an email when your course goes live.
            </p>
          </div>

          <div className="card" style={{ padding: '16px 18px' }}>
            <div className="muted" style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 8 }}>Visibility</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6, fontSize: 13, color: 'var(--ink)' }}>
              <LockIcon /> Private draft
            </div>
            <p className="muted" style={{ fontSize: 12, margin: 0 }}>Only you can see this course until it's approved.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
