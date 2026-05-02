package io.stablepay.api.infrastructure.security;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.UserId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;

@Builder(toBuilder = true)
public record AuthenticatedUser(
    UserId userId, Optional<CustomerId> customerId, Set<Role> roles, String email) {

  public AuthenticatedUser {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(roles, "roles");
    Objects.requireNonNull(email, "email");
    roles = Set.copyOf(roles);
  }

  public boolean isAdmin() {
    return roles.contains(Role.ADMIN);
  }

  public boolean isCustomer() {
    return roles.contains(Role.CUSTOMER);
  }

  public boolean isAgent() {
    return roles.contains(Role.AGENT);
  }

  public CustomerId requireCustomerId() {
    return customerId.orElseThrow(
        () -> new IllegalStateException("Endpoint requires CUSTOMER role"));
  }
}
