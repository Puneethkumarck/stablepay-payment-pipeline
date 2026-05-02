package io.stablepay.api.infrastructure.opensearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder(toBuilder = true)
public record OpenSearchTransactionDocument(
    @JsonProperty("event_id") String eventId,
    @JsonProperty("transaction_reference") String transactionReference,
    @JsonProperty("flow_type") String flowType,
    @JsonProperty("internal_status") String internalStatus,
    @JsonProperty("customer_status") String customerStatus,
    @JsonProperty("amount_micros") long amountMicros,
    @JsonProperty("currency_code") String currencyCode,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("account_id") String accountId,
    @JsonProperty("flow_id") String flowId,
    @JsonProperty("correlation_id") String correlationId,
    @JsonProperty("trace_id") String traceId,
    @JsonProperty("event_time") long eventTimeEpochMillis,
    @JsonProperty("ingest_time") long ingestTimeEpochMillis) {}
