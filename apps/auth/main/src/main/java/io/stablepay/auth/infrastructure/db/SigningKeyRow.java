package io.stablepay.auth.infrastructure.db;

import java.time.Instant;

public record SigningKeyRow(
    String kid,
    String privateKeyPem,
    String publicKeyPem,
    String algorithm,
    Instant createdAt,
    boolean isActive) {}
