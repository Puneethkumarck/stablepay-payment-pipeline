package io.stablepay.auth.infrastructure.db;

import java.time.Instant;
import java.util.UUID;

public record UserRow(
    UUID userId,
    UUID customerId,
    String email,
    String password,
    String roles,
    Instant createdAt,
    Instant updatedAt) {}
