package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record Flow(
    FlowId id,
    String flowType,
    String status,
    CustomerId customerId,
    Money totalAmount,
    int legCount,
    Instant createdAt,
    Instant updatedAt,
    Optional<Instant> completedAt) {

  public Flow {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(totalAmount, "totalAmount");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    Objects.requireNonNull(completedAt, "completedAt");
    if (legCount < 1) {
      throw new IllegalArgumentException("legCount must be >= 1");
    }
  }
}
