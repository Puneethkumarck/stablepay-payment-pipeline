package io.stablepay.auth.infrastructure.db;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenRow(
    UUID tokenId,
    UUID userId,
    String tokenHash,
    Instant issuedAt,
    Instant expiresAt,
    Instant revokedAt) {}
