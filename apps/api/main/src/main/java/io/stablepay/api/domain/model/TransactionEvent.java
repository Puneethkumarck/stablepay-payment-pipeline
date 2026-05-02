package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransactionEvent(
    String eventId,
    String customerId,
    String status,
    Instant eventTime,
    long amountMicros,
    String currencyCode,
    String flowType,
    String sortKey) {

  public TransactionEvent {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(eventTime, "eventTime");
    Objects.requireNonNull(currencyCode, "currencyCode");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(sortKey, "sortKey");
  }
}
