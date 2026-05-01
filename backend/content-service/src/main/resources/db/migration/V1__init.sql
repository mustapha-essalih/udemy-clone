CREATE TABLE media_files (
    id UUID PRIMARY KEY,
    owner_id UUID,
    file_name TEXT,
    file_type VARCHAR(20),
    mime_type VARCHAR(100),
    size BIGINT,
    storage_provider VARCHAR(10),
    storage_key TEXT,
    url TEXT,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE upload_sessions (
    id UUID PRIMARY KEY,
    media_id UUID,
    upload_type VARCHAR(20),
    chunk_index INT,
    total_chunks INT,
    status VARCHAR(20)
);

CREATE INDEX idx_media_owner ON media_files(owner_id);
