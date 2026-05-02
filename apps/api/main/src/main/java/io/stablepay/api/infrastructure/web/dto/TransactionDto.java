package io.stablepay.api.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransactionDto(
    String id,
    String reference,
    @JsonProperty("flow_type") String flowType,
    @JsonProperty("internal_status") String internalStatus,
    @JsonProperty("customer_status") String customerStatus,
    AmountDto amount,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("account_id") String accountId,
    Optional<String> counterparty,
    @JsonProperty("flow_id") String flowId,
    @JsonProperty("event_time") Instant eventTime,
    @JsonProperty("ingest_time") Instant ingestTime) {

  public TransactionDto {
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
    Objects.requireNonNull(eventTime, "eventTime");
    Objects.requireNonNull(ingestTime, "ingestTime");
  }
}
