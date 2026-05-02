package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransactionSearch(
    Optional<String> reference,
    Optional<String> flowType,
    Optional<String> internalStatus,
    Optional<String> customerStatus,
    Optional<Instant> from,
    Optional<Instant> to,
    int pageSize,
    Optional<String> cursor) {

  public TransactionSearch {
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(internalStatus, "internalStatus");
    Objects.requireNonNull(customerStatus, "customerStatus");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(cursor, "cursor");
  }
}
