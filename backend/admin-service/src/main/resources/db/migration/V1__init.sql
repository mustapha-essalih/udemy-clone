CREATE TABLE IF NOT EXISTS course_reviews_queue (
    id UUID PRIMARY KEY,
    course_id UUID,
    status VARCHAR(20),
    feedback TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY,
    target_type VARCHAR(20),
    target_id UUID,
    reason TEXT,
    status VARCHAR(20)
);
