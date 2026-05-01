CREATE TABLE refresh_tokens (
    token_id    UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(user_id),
    token_hash  VARCHAR(64) NOT NULL,
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash_active ON refresh_tokens (token_hash) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_expires_at_active ON refresh_tokens (expires_at) WHERE revoked_at IS NULL;
