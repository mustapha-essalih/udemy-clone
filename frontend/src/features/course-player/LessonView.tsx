import type { LessonResponse } from '../../api/courses';

interface Props {
  lesson: LessonResponse | null;
}

export default function LessonView({ lesson }: Props) {
  if (!lesson) {
    return (
      <div className="player-main">
        <div className="video-placeholder">
          <p className="muted">Select a lesson to begin.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="player-main">
      {lesson.lessonType === 'VIDEO' ? (
        lesson.videoUrl ? (
          <div className="video-container">
            <video key={lesson.lessonId} controls preload="metadata">
              <source src={lesson.videoUrl} />
              Your browser does not support video playback.
            </video>
          </div>
        ) : (
          <div className="video-placeholder">
            <p className="muted">Video not yet available.</p>
          </div>
        )
      ) : (
        <div className="text-content-placeholder">
          <span className="text-lesson-icon">≡</span>
          <p className="muted">Text lesson</p>
          {lesson.videoUrl && (
            <a
              href={lesson.videoUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary"
            >
              Open resource ↗
            </a>
          )}
        </div>
      )}

      <div className="lesson-detail">
        <h2>{lesson.title}</h2>
        <div className="lesson-detail-meta">
          {lesson.isPreview && <span className="media-tag">Free Preview</span>}
          <span className="muted small">{lesson.lessonType}</span>
          {lesson.durationMinutes != null && (
            <span className="muted small">{lesson.durationMinutes} min</span>
          )}
        </div>
      </div>
    </div>
  );
}
