CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    user_id UUID,
    course_id UUID,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_reviews_course ON reviews(course_id);
