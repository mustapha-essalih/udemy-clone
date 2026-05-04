CREATE TABLE IF NOT EXISTS media_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    course_id UUID,
    section_id UUID,
    lesson_id UUID,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    file_type VARCHAR(20),
    mime_type VARCHAR(100),
    size BIGINT,
    local_path TEXT,
    storage_key TEXT,
    url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    storage_provider VARCHAR(10) NOT NULL DEFAULT 'LOCAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS upload_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    course_id UUID NOT NULL,
    section_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    course_name VARCHAR(255),
    section_name VARCHAR(255),
    lesson_title VARCHAR(255),
    total_size BIGINT NOT NULL,
    chunk_size BIGINT NOT NULL,
    total_chunks INTEGER NOT NULL,
    uploaded_chunks INTEGER NOT NULL DEFAULT 0,
    uploaded_bytes BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    temp_path TEXT,
    media_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX IF NOT EXISTS idx_media_files_lesson ON media_files(lesson_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_lesson ON upload_sessions(lesson_id);
