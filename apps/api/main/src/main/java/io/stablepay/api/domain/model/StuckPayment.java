package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record StuckPayment(
    TransactionId id,
    String reference,
    String flowType,
    String internalStatus,
    CustomerId customerId,
    Money amount,
    Instant lastEventAt,
    long stuckMillis) {

  public StuckPayment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(internalStatus, "internalStatus");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(lastEventAt, "lastEventAt");
    if (stuckMillis < 0) {
      throw new IllegalArgumentException("stuckMillis must be >= 0");
    }
  }
}
