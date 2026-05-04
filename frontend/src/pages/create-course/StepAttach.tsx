import type { UploadTarget, UploadState } from './types';
import { fmtBytes } from './types';

const CheckIcon = () => (
  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M5 12l5 5L20 7"/>
  </svg>
);
const WarnIcon = () => (
  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 8v5M12 16.5v.5M3 19h18L12 3z"/>
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
const ChevRightIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M9 6l6 6-6 6"/>
  </svg>
);

interface FlatLesson {
  target: UploadTarget;
  state: UploadState;
  sidx: number;
  lidx: number;
}

interface Props {
  targets: UploadTarget[];
  uploads: Record<string, UploadState>;
  setActiveLessonId: (id: string) => void;
  goTo: (step: number) => void;
}

function StatusPill({ state }: { state: UploadState }) {
  if (state.status === 'uploading') {
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 160 }}>
        <div style={{ flex: 1, height: 4, background: 'var(--bg-2)', borderRadius: 999, overflow: 'hidden' }}>
          <div style={{ height: '100%', background: 'var(--amber)', width: `${state.percent}%`, transition: 'width 200ms' }} />
        </div>
        <span className="mono muted" style={{ fontSize: 11, minWidth: 32, textAlign: 'right' }}>{Math.round(state.percent)}%</span>
      </div>
    );
  }
  const map: Record<UploadState['status'], { mod: string; label: string; icon: React.ReactNode }> = {
    idle:      { mod: '',        label: 'Awaiting file', icon: <span className="pill-dot" /> },
    starting:  { mod: ' amber', label: 'Starting…',    icon: <span className="pill-dot" /> },
    paused:    { mod: '',        label: 'Paused',       icon: <span className="pill-dot" /> },
    done:      { mod: ' green', label: 'Completed',     icon: <CheckIcon /> },
    error:     { mod: ' red',   label: 'Failed',        icon: <WarnIcon /> },
    uploading: { mod: ' amber', label: 'Uploading',     icon: <span className="pill-dot" /> },
  };
  const m = map[state.status];
  return <span className={`pill${m.mod}`}>{m.icon} {m.label}</span>;
}

export default function StepAttach({ targets, uploads, setActiveLessonId, goTo }: Props) {
  type SectionGroup = { sectionId: string; sectionTitle: string; lessons: FlatLesson[] };
  const groups: SectionGroup[] = [];
  const seenSections: string[] = [];
  targets.forEach((t) => {
    if (!seenSections.includes(t.sectionId)) {
      seenSections.push(t.sectionId);
      groups.push({ sectionId: t.sectionId, sectionTitle: t.sectionTitle, lessons: [] });
    }
    const g = groups.find(g => g.sectionId === t.sectionId)!;
    const sidx = seenSections.indexOf(t.sectionId);
    g.lessons.push({ target: t, state: uploads[t.lessonId] ?? { file: null, sessionId: null, status: 'idle', percent: 0, speed: 0, errorMsg: '', linkedUrl: null }, sidx, lidx: g.lessons.length });
  });

  const flat = groups.flatMap(g => g.lessons);
  const counts = flat.reduce((acc, l) => { acc[l.state.status] = (acc[l.state.status] || 0) + 1; return acc; }, {} as Record<string, number>);

  return (
    <div className="step-content">
      <div className="card card-pad" style={{ marginBottom: 20 }}>
        <div className="card-h">
          <h3>Attached content</h3>
          <span className="count">{flat.length} lessons total</span>
        </div>
        <p className="card-desc">Each uploaded file is linked back to its lesson. Click any row to revisit the upload.</p>

        <div className="row-gap">
          <span className="pill green"><CheckIcon /> {counts.done || 0} completed</span>
          <span className="pill amber"><span className="pill-dot" /> {counts.uploading || 0} uploading</span>
          <span className="pill"><span className="pill-dot" /> {counts.idle || 0} awaiting</span>
          {(counts.error || 0) > 0 && (
            <span className="pill red"><WarnIcon /> {counts.error} failed</span>
          )}
          <span className="pill">
            {flat.length ? Math.round(((counts.done || 0) / flat.length) * 100) : 0}% complete
          </span>
        </div>
      </div>

      <div className="card" style={{ overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: 'var(--bg-2)', borderBottom: '1px solid var(--hairline)' }}>
              {['#', 'Lesson', 'Type', 'File', 'Size', 'Status', ''].map((h, i) => (
                <th
                  key={i}
                  style={{
                    padding: '10px 16px',
                    fontSize: 11,
                    fontWeight: 500,
                    color: 'var(--ink-3)',
                    letterSpacing: '0.04em',
                    textTransform: 'uppercase',
                    textAlign: i === 0 || i === 4 ? 'right' : 'left',
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {flat.map((item) => (
              <tr
                key={item.target.lessonId}
                style={{ borderBottom: '1px solid var(--hairline)', cursor: 'pointer' }}
                onClick={() => { setActiveLessonId(item.target.lessonId); goTo(2); }}
                onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--bg-2)')}
                onMouseLeave={(e) => (e.currentTarget.style.background = '')}
              >
                <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                  <span className="mono muted" style={{ fontSize: 11 }}>
                    {String(item.sidx + 1).padStart(2, '0')}.{String(item.lidx + 1).padStart(2, '0')}
                  </span>
                </td>
                <td style={{ padding: '14px 16px' }}>
                  <div style={{ fontSize: 13.5, fontWeight: 500, color: 'var(--ink)' }}>{item.target.lessonTitle}</div>
                  <div className="muted" style={{ fontSize: 11.5 }}>{item.target.sectionTitle}</div>
                </td>
                <td style={{ padding: '14px 16px' }}>
                  <span style={{
                    width: 24, height: 24, borderRadius: 6,
                    display: 'inline-grid', placeItems: 'center',
                    background: item.target.type === 'video' ? 'var(--accent-soft)' : 'var(--hot-soft)',
                    color: item.target.type === 'video' ? 'var(--accent-ink)' : 'var(--hot)',
                  }}>
                    {item.target.type === 'video' ? <VideoIcon /> : item.target.type === 'pdf' ? <PdfIcon /> : <ArticleIcon />}
                  </span>
                </td>
                <td style={{ padding: '14px 16px' }}>
                  <span className="mono" style={{ fontSize: 12, color: item.state.file ? 'var(--ink-2)' : 'var(--ink-4)' }}>
                    {item.state.file?.name ?? '—'}
                  </span>
                </td>
                <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                  <span className="mono" style={{ fontSize: 12, color: item.state.file ? 'var(--ink-2)' : 'var(--ink-4)' }}>
                    {item.state.file ? fmtBytes(item.state.file.size) : '—'}
                  </span>
                </td>
                <td style={{ padding: '14px 16px' }}>
                  <StatusPill state={item.state} />
                </td>
                <td style={{ padding: '14px 16px', color: 'var(--ink-4)' }}>
                  <ChevRightIcon />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
