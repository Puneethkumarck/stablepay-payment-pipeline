-- SPP-88: idempotency-as-replay cache (CONTEXT.md D-B2/D-B3)
-- Composite PK on (idempotency_key, user_id) prevents cross-customer key collision.
-- TTL enforced via expires_at; daily Spring @Scheduled cleanup truncates expired rows.
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(64) NOT NULL,
    user_id         UUID NOT NULL,
    response_status INT NOT NULL,
    response_body   BYTEA,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (idempotency_key, user_id)
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
