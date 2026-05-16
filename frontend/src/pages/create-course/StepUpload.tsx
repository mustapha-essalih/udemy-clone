import { useRef, useState } from 'react';
import type { UploadTarget, UploadState } from './types';
import { fmtBytes, fmtSpeed, fmtETA } from './types';

const UploadIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 16V4M6 10l6-6 6 6M4 18v1a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-1"/>
  </svg>
);
const VideoIcon = ({ size = 16 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 7a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM16 9.5l5-3v11l-5-3z"/>
  </svg>
);
const PdfIcon = ({ size = 16 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M7 3h7l5 5v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM14 3v5h5"/>
  </svg>
);
const PauseIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
    <rect x="7" y="5" width="3" height="14" rx="1"/><rect x="14" y="5" width="3" height="14" rx="1"/>
  </svg>
);
const PlayIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M7 5v14l12-7z"/>
  </svg>
);
const XIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
    <path d="M6 6l12 12M18 6l-12 12"/>
  </svg>
);
const CheckIcon = () => (
  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 12l5 5L20 7"/>
  </svg>
);
const EyeIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z"/>
  </svg>
);
const ArticleIcon = ({ size = 16 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M4 6h16M4 10h16M4 14h10M4 18h8"/>
  </svg>
);

export interface StepUploadHandlers {
  onFileSelect: (lessonId: string, file: File) => void;
  onStart: (lessonId: string) => void;
  onPause: (lessonId: string) => void;
  onResume: (lessonId: string) => void;
  onCancel: (lessonId: string) => void;
  onReupload: (lessonId: string) => void;
}

interface Props extends StepUploadHandlers {
  targets: UploadTarget[];
  uploads: Record<string, UploadState>;
  activeLessonId: string | null;
  setActiveLessonId: (id: string) => void;
}

export default function StepUpload({ targets, uploads, activeLessonId, setActiveLessonId, onFileSelect, onStart, onPause, onResume, onCancel, onReupload }: Props) {
  const active = targets.find((t) => t.lessonId === activeLessonId) ?? targets[0] ?? null;

  type Group = { sectionId: string; sectionTitle: string; lessons: UploadTarget[] };
  const groups = Object.values(
    targets.reduce((acc, t) => {
      if (!acc[t.sectionId]) acc[t.sectionId] = { sectionId: t.sectionId, sectionTitle: t.sectionTitle, lessons: [] };
      acc[t.sectionId].lessons.push(t);
      return acc;
    }, {} as Record<string, Group>)
  );

  if (targets.length === 0) {
    return (
      <div className="step-content">
        <div className="card card-pad" style={{ textAlign: 'center' }}>
          <div className="card-h" style={{ justifyContent: 'center' }}>
            <h3>No lessons yet</h3>
          </div>
          <p className="muted">Go back to Curriculum to add sections and lessons.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="step-content">
      <div className="upload-shell">
        {}
        <div className="card lesson-list-card">
          <div className="lesson-list-h">Lessons · pick to upload</div>
          {groups.map((g, sidx) => (
            <div key={g.sectionId} className="ll-section">
              <div className="ll-section-title">
                <span>{String(sidx + 1).padStart(2, '0')} · {g.sectionTitle}</span>
                <small>{g.lessons.filter(l => uploads[l.lessonId]?.status === 'done').length}/{g.lessons.length}</small>
              </div>
              {g.lessons.map((l, lidx) => {
                const state = uploads[l.lessonId];
                const isActive = l.lessonId === active?.lessonId;
                const st = state?.status ?? 'idle';
                return (
                  <div
                    key={l.lessonId}
                    className={`ll-lesson${isActive ? ' active' : ''}`}
                    onClick={() => setActiveLessonId(l.lessonId)}
                  >
                    <span className={`ll-status-dot${st !== 'idle' ? ` ${st}` : ''}`} />
                    <span className="ll-lesson-num">
                      {String(sidx + 1).padStart(2, '0')}.{String(lidx + 1).padStart(2, '0')}
                    </span>
                    <span className="ll-lesson-title">{l.lessonTitle}</span>
                    <span style={{ color: 'var(--ink-4)', flexShrink: 0 }}>
                      {l.type === 'pdf' ? <PdfIcon size={12} /> : <VideoIcon size={12} />}
                    </span>
                  </div>
                );
              })}
            </div>
          ))}
        </div>

        {}
        {active ? (
          <UploadPane
            target={active}
            state={uploads[active.lessonId] ?? { file: null, sessionId: null, status: 'idle', percent: 0, speed: 0, errorMsg: '', linkedUrl: null }}
            onFileSelect={(f) => onFileSelect(active.lessonId, f)}
            onStart={() => onStart(active.lessonId)}
            onPause={() => onPause(active.lessonId)}
            onResume={() => onResume(active.lessonId)}
            onCancel={() => onCancel(active.lessonId)}
            onReupload={() => onReupload(active.lessonId)}
          />
        ) : (
          <div className="card card-pad" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <span className="muted">Select a lesson to upload</span>
          </div>
        )}
      </div>
    </div>
  );
}

interface UploadPaneProps {
  target: UploadTarget;
  state: UploadState;
  onFileSelect: (f: File) => void;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onCancel: () => void;
  onReupload: () => void;
}

function StatusPill({ status }: { status: UploadState['status'] }) {
  const map: Record<UploadState['status'], { mod: string; label: string }> = {
    idle:      { mod: '',        label: 'Awaiting file' },
    starting:  { mod: ' amber', label: 'Starting…' },
    uploading: { mod: ' amber', label: 'Uploading' },
    paused:    { mod: '',       label: 'Paused' },
    done:      { mod: ' green', label: 'Completed' },
    error:     { mod: ' red',   label: 'Failed' },
  };
  const m = map[status];
  return (
    <span className={`pill${m.mod}`}>
      <span className="pill-dot" />
      {m.label}
    </span>
  );
}

function UploadPane({ target, state, onFileSelect, onStart, onPause, onResume, onCancel, onReupload }: UploadPaneProps) {
  const [drag, setDrag] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const isVideo = target.type === 'video';
  const isPdf = target.type === 'pdf';

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDrag(false);
    const f = e.dataTransfer.files[0];
    if (f) onFileSelect(f);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {}
      <div className="lesson-info-bar">
        <span className={`lesson-info-icon${isVideo ? ' video' : ' pdf'}`}>
          {isVideo ? <VideoIcon /> : isPdf ? <PdfIcon /> : <ArticleIcon />}
        </span>
        <div>
          <div className="lesson-info-section">{target.sectionTitle}</div>
          <div className="lesson-info-title">{target.lessonTitle}</div>
        </div>
        {state.status !== 'idle' && (
          <div style={{ marginLeft: 'auto' }}>
            <StatusPill status={state.status} />
          </div>
        )}
      </div>

      {}
      {state.status === 'idle' && !state.file && (
        <div
          className={`dropzone${drag ? ' drag' : ''}`}
          onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
          onDragLeave={() => setDrag(false)}
          onDrop={handleDrop}
        >
          <div className="dropzone-icon"><UploadIcon /></div>
          <h4>Drop your {isVideo ? 'video' : isPdf ? 'PDF' : 'article'} here</h4>
          <p>or browse to select a file from your computer</p>
          <button type="button" className="btn accent" onClick={() => inputRef.current?.click()}>
            <UploadIcon /> Choose file
          </button>
          <input
            ref={inputRef}
            type="file"
            hidden
            accept={isVideo ? 'video}
      {state.status === 'idle' && state.file && (
        <div className="upload-progress-card">
          <div className="upload-row">
            <span className={`file-thumb${isVideo ? '' : ' pdf'}`}>
              {isVideo ? <VideoIcon size={20} /> : isPdf ? <PdfIcon size={20} /> : <ArticleIcon size={20} />}
            </span>
            <div className="upload-meta">
              <div className="upload-name">{state.file.name}</div>
              <div className="upload-stats">
                <span className="mono">{fmtBytes(state.file.size)}</span>
              </div>
            </div>
            <div className="upload-controls">
              <button type="button" className="icon-btn danger" onClick={onCancel}><XIcon /></button>
            </div>
          </div>
          <div style={{ marginTop: 14 }}>
            <button type="button" className="btn accent" onClick={onStart}>
              <UploadIcon /> Start upload
            </button>
          </div>
        </div>
      )}

      {}
      {(state.status === 'uploading' || state.status === 'starting' || state.status === 'paused' || state.status === 'done' || state.status === 'error') && state.file && (
        <div className="upload-progress-card">
          <div className="upload-row">
            <span className={`file-thumb${isVideo ? '' : ' pdf'}`}>
              {isVideo ? <VideoIcon size={20} /> : <PdfIcon size={20} />}
            </span>
            <div className="upload-meta">
              <div className="upload-name">{state.file.name}</div>
              <div className="upload-stats">
                <span>{fmtBytes(state.file.size * (state.percent / 100))}</span>
                <span className="sep">/</span>
                <span>{fmtBytes(state.file.size)}</span>
                {state.status === 'uploading' && state.speed > 0 && (
                  <>
                    <span className="sep">·</span>
                    <span>{fmtSpeed(state.speed)}</span>
                    <span className="sep">·</span>
                    <span>ETA {fmtETA((state.file.size * (1 - state.percent / 100)) / state.speed)}</span>
                  </>
                )}
                {state.status === 'done' && <><span className="sep">·</span><span style={{ color: 'var(--green)' }}>Upload complete</span></>}
                {state.status === 'paused' && <><span className="sep">·</span><span>Paused</span></>}
                {state.status === 'error' && <><span className="sep">·</span><span style={{ color: 'var(--red)' }}>{state.errorMsg || 'Upload failed'}</span></>}
              </div>
            </div>
            <div className="upload-controls">
              {state.status === 'uploading' && (
                <>
                  <button type="button" className="icon-btn" title="Pause" onClick={onPause}><PauseIcon /></button>
                  <button type="button" className="icon-btn danger" title="Cancel" onClick={onCancel}><XIcon /></button>
                </>
              )}
              {state.status === 'paused' && (
                <>
                  <button type="button" className="icon-btn" title="Resume" onClick={onResume}><PlayIcon /></button>
                  <button type="button" className="icon-btn danger" title="Cancel" onClick={onCancel}><XIcon /></button>
                </>
              )}
              {state.status === 'error' && (
                <button type="button" className="btn sm" onClick={onReupload}>Re-upload</button>
              )}
              {state.status === 'done' && (
                <button type="button" className="icon-btn danger" title="Replace" onClick={onCancel}><XIcon /></button>
              )}
            </div>
          </div>

          <div className="progress-bar">
            <div
              className={`progress-fill${state.status === 'error' ? ' failed' : state.status === 'done' ? ' done' : ''}`}
              style={{ width: `${state.percent}%` }}
            />
          </div>
        </div>
      )}

      {}
      {state.status === 'done' && state.linkedUrl && (
        <div className="upload-progress-card">
          <div className="upload-row">
            <span className="pill green"><CheckIcon /> Linked to lesson</span>
            <span className="mono muted" style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 11, marginLeft: 12 }}>
              {state.file?.name} · {fmtBytes(state.file?.size ?? 0)}
            </span>
            <div className="upload-controls">
              <button type="button" className="btn sm"><EyeIcon /> Preview</button>
              <button type="button" className="btn sm" onClick={onCancel}>Replace</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
