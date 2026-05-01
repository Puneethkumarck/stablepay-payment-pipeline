package io.stablepay.auth.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;

@Builder(toBuilder = true)
public record User(
    UserId id,
    Optional<CustomerId> customerId,
    String email,
    String passwordHash,
    Set<Role> roles,
    Instant createdAt,
    Instant updatedAt) {}
