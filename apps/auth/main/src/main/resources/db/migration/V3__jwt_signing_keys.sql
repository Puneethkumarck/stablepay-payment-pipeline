CREATE TABLE jwt_signing_keys (
    kid             VARCHAR(64) PRIMARY KEY,
    private_key_pem TEXT NOT NULL,
    public_key_pem  TEXT NOT NULL,
    algorithm       VARCHAR(16) NOT NULL DEFAULT 'RS256',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);
