CREATE TABLE ingestion_runs (
    id                 BIGSERIAL PRIMARY KEY,
    data_set           VARCHAR(100) NOT NULL,
    source_type        VARCHAR(50) NOT NULL,
    status             VARCHAR(30) NOT NULL DEFAULT 'RUNNING',
    total_discovered   INT NOT NULL DEFAULT 0,
    downloaded         INT NOT NULL DEFAULT 0,
    failed             INT NOT NULL DEFAULT 0,
    skipped_duplicate  INT NOT NULL DEFAULT 0,
    started_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_ingestion_runs_data_set ON ingestion_runs (data_set);
CREATE INDEX idx_ingestion_runs_status ON ingestion_runs (status);
CREATE INDEX idx_ingestion_runs_started_at ON ingestion_runs (started_at);
