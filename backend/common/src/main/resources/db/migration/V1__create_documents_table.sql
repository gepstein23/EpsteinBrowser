CREATE TABLE documents (
    id              BIGSERIAL PRIMARY KEY,
    source_url      TEXT NOT NULL,
    file_hash       VARCHAR(64) NOT NULL,
    s3_raw_key      VARCHAR(512) NOT NULL,
    data_set        VARCHAR(100) NOT NULL,
    file_name       VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT,
    processing_state VARCHAR(30) NOT NULL DEFAULT 'INGESTED',
    ingested_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_documents_file_hash ON documents (file_hash);
CREATE INDEX idx_documents_data_set ON documents (data_set);
CREATE INDEX idx_documents_processing_state ON documents (processing_state);
CREATE INDEX idx_documents_ingested_at ON documents (ingested_at);
