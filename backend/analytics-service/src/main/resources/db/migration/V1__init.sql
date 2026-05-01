CREATE TABLE course_stats (
    course_id UUID PRIMARY KEY,
    total_enrollments INT,
    total_revenue NUMERIC,
    avg_rating NUMERIC
);

CREATE TABLE user_activity (
    user_id UUID PRIMARY KEY,
    last_login TIMESTAMP,
    total_courses_enrolled INT
);
