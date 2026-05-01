CREATE TABLE courses (
    id UUID PRIMARY KEY,
    instructor_id UUID,
    title TEXT,
    subtitle TEXT,
    description TEXT,
    language VARCHAR(20),
    level VARCHAR(20),
    price NUMERIC,
    status VARCHAR(20),
    thumbnail_url TEXT,
    total_duration INT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE sections (
    id UUID PRIMARY KEY,
    course_id UUID,
    title TEXT,
    order_index INT
);

CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    section_id UUID,
    title TEXT,
    lesson_type VARCHAR(20),
    is_preview BOOLEAN,
    duration INT,
    order_index INT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE lesson_contents (
    id UUID PRIMARY KEY,
    lesson_id UUID UNIQUE,
    content_type VARCHAR(20),
    text_content TEXT,
    primary_media_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE lesson_resources (
    id UUID PRIMARY KEY,
    lesson_id UUID,
    media_id UUID,
    title TEXT,
    type VARCHAR(20)
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    parent_id UUID
);

CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_sections_course ON sections(course_id);
CREATE INDEX idx_lessons_section ON lessons(section_id);
