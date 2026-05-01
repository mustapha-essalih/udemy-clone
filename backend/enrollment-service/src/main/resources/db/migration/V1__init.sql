CREATE TABLE enrollments (
    id UUID PRIMARY KEY,
    user_id UUID,
    course_id UUID,
    enrolled_at TIMESTAMP DEFAULT NOW(),
    status VARCHAR(20)
);

CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY,
    enrollment_id UUID,
    lesson_id UUID,
    completed BOOLEAN,
    progress_percent INT,
    last_position_seconds INT,
    updated_at TIMESTAMP
);

CREATE INDEX idx_enrollment_user ON enrollments(user_id);
CREATE INDEX idx_progress_enrollment ON lesson_progress(enrollment_id);
