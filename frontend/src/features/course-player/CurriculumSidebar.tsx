import { useState } from 'react';
import type { SectionResponse, LessonResponse } from '../../api/courses';

interface Props {
  sections: SectionResponse[];
  activeLesson: LessonResponse | null;
  onSelect: (lesson: LessonResponse) => void;
}

export default function CurriculumSidebar({ sections, activeLesson, onSelect }: Props) {
  const [expanded, setExpanded] = useState<Record<string, boolean>>(
    Object.fromEntries(sections.map((s) => [s.sectionId, true]))
  );

  const toggle = (id: string) =>
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));

  return (
    <aside className="curriculum-sidebar">
      <div className="sidebar-heading">Curriculum</div>

      {sections.length === 0 && (
        <p className="muted" style={{ padding: '20px 18px' }}>No content yet.</p>
      )}

      {sections.map((section, si) => (
        <div key={section.sectionId} className="curriculum-section">
          <button
            className="curriculum-section-toggle"
            onClick={() => toggle(section.sectionId)}
          >
            <span className="toggle-arrow">{expanded[section.sectionId] ? '▾' : '▸'}</span>
            <span style={{ flex: 1 }}>
              Section {si + 1}: {section.title}
            </span>
            <span className="lesson-btn-meta">{section.lessons.length}</span>
          </button>

          {expanded[section.sectionId] &&
            section.lessons.map((lesson) => {
              const isActive = activeLesson?.lessonId === lesson.lessonId;
              return (
                <button
                  key={lesson.lessonId}
                  className={`curriculum-lesson-btn${isActive ? ' active' : ''}`}
                  onClick={() => onSelect(lesson)}
                >
                  <span className="lesson-type-icon">
                    {lesson.lessonType === 'VIDEO' ? '▶' : '≡'}
                  </span>
                  <span className="lesson-btn-title">{lesson.title}</span>
                  {lesson.isPreview && <span className="preview-tag">Preview</span>}
                  {lesson.durationMinutes != null && (
                    <span className="lesson-btn-meta">{lesson.durationMinutes}m</span>
                  )}
                </button>
              );
            })}
        </div>
      ))}
    </aside>
  );
}
