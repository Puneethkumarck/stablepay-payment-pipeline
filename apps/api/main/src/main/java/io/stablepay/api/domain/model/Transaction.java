package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record Transaction(
    TransactionId id,
    String reference,
    String flowType,
    String internalStatus,
    String customerStatus,
    Money amount,
    CustomerId customerId,
    AccountId accountId,
    Optional<String> counterparty,
    FlowId flowId,
    String eventId,
    String correlationId,
    String traceId,
    Instant eventTime,
    Instant ingestTime,
    Map<String, Object> typedFields) {

  public Transaction {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(internalStatus, "internalStatus");
    Objects.requireNonNull(customerStatus, "customerStatus");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(accountId, "accountId");
    Objects.requireNonNull(counterparty, "counterparty");
    Objects.requireNonNull(flowId, "flowId");
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(traceId, "traceId");
    Objects.requireNonNull(eventTime, "eventTime");
    Objects.requireNonNull(ingestTime, "ingestTime");
    Objects.requireNonNull(typedFields, "typedFields");
    typedFields = Map.copyOf(typedFields);
  }
}
