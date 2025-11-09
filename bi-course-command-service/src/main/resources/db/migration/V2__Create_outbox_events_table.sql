-- Create outbox_events table for reliable event publishing
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(500)
);

-- Create composite index for efficient polling of pending events
CREATE INDEX idx_status_created ON outbox_events(status, created_at);

-- Create index on aggregate_id for tracking events by entity
CREATE INDEX idx_aggregate_id ON outbox_events(aggregate_id);

-- Add comment for documentation
COMMENT ON TABLE outbox_events IS 'Outbox pattern for reliable event publishing to Kafka';
COMMENT ON COLUMN outbox_events.status IS 'PENDING, PUBLISHED, or FAILED';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of publish attempts';
