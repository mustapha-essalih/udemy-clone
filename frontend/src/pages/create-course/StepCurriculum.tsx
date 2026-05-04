import { useState } from 'react';
import type { DraftSection, DraftLesson } from './types';

type LessonType = DraftLesson['type'];

const GripIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" stroke="none">
    <circle cx="9" cy="6" r="1.5"/><circle cx="9" cy="12" r="1.5"/><circle cx="9" cy="18" r="1.5"/>
    <circle cx="15" cy="6" r="1.5"/><circle cx="15" cy="12" r="1.5"/><circle cx="15" cy="18" r="1.5"/>
  </svg>
);
const VideoIcon = ({ size = 14 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 7a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM16 9.5l5-3v11l-5-3z"/>
  </svg>
);
const PdfIcon = ({ size = 14 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M7 3h7l5 5v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM14 3v5h5"/>
  </svg>
);
const ArticleIcon = ({ size = 14 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M4 6h16M4 10h16M4 14h10M4 18h8"/>
  </svg>
);
const TrashIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M4 7h16M9 7V4h6v3M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13"/>
  </svg>
);
const PlusIcon = ({ size = 12 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <path d="M12 5v14M5 12h14"/>
  </svg>
);
const ChevRightIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M9 6l6 6-6 6"/>
  </svg>
);

let draftCounter = 100;
const newId = () => `d${++draftCounter}`;

interface Props {
  sections: DraftSection[];
  setSections: React.Dispatch<React.SetStateAction<DraftSection[]>>;
}

export default function StepCurriculum({ sections, setSections }: Props) {
  const [openIds, setOpenIds] = useState<string[]>(() => sections.map((s) => s.draftId));

  const toggleOpen = (id: string) =>
    setOpenIds((ids) => ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]);

  const updateSection = (id: string, title: string) =>
    setSections((sx) => sx.map((s) => s.draftId === id ? { ...s, title } : s));

  const removeSection = (id: string) =>
    setSections((sx) => sx.filter((s) => s.draftId !== id));

  const addSection = () => {
    const id = newId();
    setSections((sx) => [...sx, { draftId: id, title: 'Untitled section', lessons: [] }]);
    setOpenIds((ids) => [...ids, id]);
  };

  const addLesson = (sectionId: string, type: LessonType) => {
    const lid = newId();
    setSections((sx) => sx.map((s) =>
      s.draftId === sectionId
        ? { ...s, lessons: [...s.lessons, { draftId: lid, title: 'New lesson', type }] }
        : s
    ));
  };

  const updateLesson = (sectionId: string, lessonId: string, patch: Partial<DraftLesson>) =>
    setSections((sx) => sx.map((s) =>
      s.draftId === sectionId
        ? { ...s, lessons: s.lessons.map((l) => l.draftId === lessonId ? { ...l, ...patch } : l) }
        : s
    ));

  const removeLesson = (sectionId: string, lessonId: string) =>
    setSections((sx) => sx.map((s) =>
      s.draftId === sectionId ? { ...s, lessons: s.lessons.filter((l) => l.draftId !== lessonId) } : s
    ));

  const totalLessons = sections.reduce((a, s) => a + s.lessons.length, 0);

  return (
    <div className="step-content">
      <div className="card card-pad" style={{ marginBottom: 20 }}>
        <div className="card-h">
          <h3>Course curriculum</h3>
          <span className="count">{sections.length} sections · {totalLessons} lessons</span>
        </div>
        <p className="card-desc">Organize your course into sections, then add lessons inside each. Click any title to rename it inline.</p>
      </div>

      {sections.map((sec, idx) => {
        const isOpen = openIds.includes(sec.draftId);
        return (
          <div key={sec.draftId} className={`section-card${isOpen ? ' open' : ''}`}>
            <div
              className="section-head"
              onClick={(e) => { if ((e.target as HTMLElement).tagName !== 'INPUT') toggleOpen(sec.draftId); }}
            >
              <span className="grip" onClick={(e) => e.stopPropagation()}><GripIcon /></span>
              <span className="section-num">SEC {String(idx + 1).padStart(2, '0')}</span>
              <input
                className="editable"
                value={sec.title}
                onChange={(e) => updateSection(sec.draftId, e.target.value)}
                onClick={(e) => e.stopPropagation()}
              />
              <span className="section-meta">{sec.lessons.length} {sec.lessons.length === 1 ? 'lesson' : 'lessons'}</span>
              <button
                type="button"
                className="icon-btn danger"
                onClick={(e) => { e.stopPropagation(); removeSection(sec.draftId); }}
              >
                <TrashIcon />
              </button>
              <span className="chev"><ChevRightIcon /></span>
            </div>

            {isOpen && (
              <div className="lessons">
                {sec.lessons.map((les) => (
                  <div key={les.draftId} className="lesson">
                    <span style={{ position: 'absolute', left: 20, color: 'var(--ink-4)', cursor: 'grab', display: 'flex' }}>
                      <GripIcon />
                    </span>
                    <span className={`lesson-icon${les.type === 'video' ? ' video' : les.type === 'pdf' ? ' pdf' : ' pdf'}`}>
                      {les.type === 'video' ? <VideoIcon /> : les.type === 'article' ? <ArticleIcon /> : <PdfIcon />}
                    </span>
                    <input
                      className="lesson-title"
                      value={les.title}
                      onChange={(e) => updateLesson(sec.draftId, les.draftId, { title: e.target.value })}
                    />
                    <div className="lesson-actions">
                      <button
                        type="button"
                        className="icon-btn"
                        title="Toggle type"
                        onClick={() => {
                          const cycle: LessonType[] = ['video', 'pdf', 'article'];
                          const next = cycle[(cycle.indexOf(les.type) + 1) % cycle.length];
                          updateLesson(sec.draftId, les.draftId, { type: next });
                        }}
                      >
                        {les.type === 'video' ? <VideoIcon /> : les.type === 'article' ? <ArticleIcon /> : <PdfIcon />}
                      </button>
                      <button
                        type="button"
                        className="icon-btn danger"
                        onClick={() => removeLesson(sec.draftId, les.draftId)}
                      >
                        <TrashIcon />
                      </button>
                    </div>
                  </div>
                ))}
                <div className="add-lesson-row">
                  <button type="button" className="add-lesson-btn" onClick={() => addLesson(sec.draftId, 'video')}>
                    <PlusIcon /> Add video
                  </button>
                  <button type="button" className="add-lesson-btn" onClick={() => addLesson(sec.draftId, 'pdf')}>
                    <PlusIcon /> Add PDF
                  </button>
                  <button type="button" className="add-lesson-btn" onClick={() => addLesson(sec.draftId, 'article')}>
                    <PlusIcon /> Add article
                  </button>
                </div>
              </div>
            )}
          </div>
        );
      })}

      <button type="button" className="add-section-btn" onClick={addSection}>
        <PlusIcon size={16} /> Add a new section
      </button>
    </div>
  );
}
