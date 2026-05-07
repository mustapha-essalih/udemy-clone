ALTER TABLE payments
    DROP COLUMN IF EXISTS stripe_payment_id,
    ADD COLUMN IF NOT EXISTS stripe_session_id VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255) UNIQUE;

CREATE TABLE IF NOT EXISTS enrollments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    payment_id UUID,
    enrolled_at TIMESTAMP DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_enrollment UNIQUE (user_id, course_id)
);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    progress_percent INT,
    last_position_seconds INT,
    updated_at TIMESTAMP,
    CONSTRAINT uq_lesson_progress UNIQUE (enrollment_id, lesson_id)
);

CREATE INDEX idx_enrollment_user ON enrollments(user_id);
CREATE INDEX idx_enrollment_course ON enrollments(course_id);
CREATE INDEX idx_lesson_progress_enrollment ON lesson_progress(enrollment_id);
