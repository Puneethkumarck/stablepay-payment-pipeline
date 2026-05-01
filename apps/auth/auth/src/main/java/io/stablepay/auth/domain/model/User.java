package io.stablepay.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
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
    Instant updatedAt) {

  public User {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(passwordHash, "passwordHash");
    Objects.requireNonNull(roles, "roles");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    roles = Set.copyOf(roles);
  }
}
