-- SPP-88: transactional outbox (CONTEXT.md D-B2; pass-3 review fix for concurrent admin replay)
-- Owns the schema today; the namastack-outbox-starter publisher (wired in a later phase)
-- reads unpublished rows from this table and emits Kafka events.
-- Two partial-unique indexes drive idempotent publish:
--   * (idempotency_key, event_topic) — same client retry collapses to one outbox row
--   * (subject_id, event_topic)      — concurrent admin replays of the same DLQ event
--                                      (different idempotency keys) collapse to one row
CREATE TABLE namastack_outbox (
    id              BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(64),
    subject_id      VARCHAR(64),
    event_topic     VARCHAR(128) NOT NULL,
    payload         BYTEA NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_outbox_idempotency_event_topic
    ON namastack_outbox (idempotency_key, event_topic)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX idx_outbox_subject_event_topic
    ON namastack_outbox (subject_id, event_topic)
    WHERE subject_id IS NOT NULL;

CREATE INDEX idx_outbox_unpublished
    ON namastack_outbox (created_at)
    WHERE published_at IS NULL;
