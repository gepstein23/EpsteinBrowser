CREATE TABLE ingestion_events (
    id          BIGSERIAL PRIMARY KEY,
    run_id      BIGINT NOT NULL REFERENCES ingestion_runs(id),
    event_type  VARCHAR(60) NOT NULL,
    document_id BIGINT REFERENCES documents(id),
    source_url  TEXT,
    message     TEXT,
    timestamp   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingestion_events_run_id ON ingestion_events (run_id);
CREATE INDEX idx_ingestion_events_event_type ON ingestion_events (event_type);
CREATE INDEX idx_ingestion_events_timestamp ON ingestion_events (timestamp);
