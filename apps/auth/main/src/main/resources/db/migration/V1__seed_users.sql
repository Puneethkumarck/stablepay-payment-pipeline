CREATE TABLE users (
    user_id      UUID PRIMARY KEY,
    customer_id  UUID,
    email        VARCHAR(255) UNIQUE NOT NULL,
    password     VARCHAR(60) NOT NULL,
    roles        VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (LOWER(email));

-- BCrypt cost 12 hashes of "demo1234"; v1 customer model: customer_id == user_id for CUSTOMER role, NULL for ADMIN/AGENT.
INSERT INTO users (user_id, customer_id, email, password, roles) VALUES
    ('11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'alice@stablepay.io', '$2a$12$EM1IpXmM8bThvR/pHFsOi.KAV.F1WpsL0/xCosKt.GfHu7c.m2JkO', 'ADMIN,CUSTOMER'),
    ('22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'bob@stablepay.io',   '$2a$12$0OMsSqcM8VbRM/g88FWD3OJOT7gqEDNTPN1XfDj2gAMRhhi7BH9B.', 'CUSTOMER'),
    ('33333333-3333-3333-3333-333333333333', NULL,                                   'admin@stablepay.io', '$2a$12$rG4YpMO6XAIOYq3AoYru1uEk0JKGJujpDbBJ6XAlN1VVNkFsKuzTy', 'ADMIN'),
    ('44444444-4444-4444-4444-444444444444', NULL,                                   'agent@stablepay.io', '$2a$12$127udczsDQn8e3fFfNFFWeHb6ohAcLHnK25jAtbLL4i.I/2zuOnzm', 'AGENT');
