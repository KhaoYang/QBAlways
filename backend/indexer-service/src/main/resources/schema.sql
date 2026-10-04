CREATE TABLE IF NOT EXISTS sync_runs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    sets_processed INTEGER NOT NULL DEFAULT 0,
    packets_processed INTEGER NOT NULL DEFAULT 0,
    documents_indexed INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS sync_runs_started_at_idx ON sync_runs (started_at DESC);
