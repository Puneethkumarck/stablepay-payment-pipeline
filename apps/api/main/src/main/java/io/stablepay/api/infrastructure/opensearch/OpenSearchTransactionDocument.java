package io.stablepay.api.infrastructure.opensearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Wire-format document for the {@code transactions} OpenSearch index.
 *
 * <p>Field names mirror the deployed index template at {@code
 * infra/opensearch/transactions-index-template.json}. Only the subset required to round-trip a
 * {@link io.stablepay.api.domain.model.Transaction} through SPP-88 is modelled here; richer payload
 * fields (chain/asset/etc.) are read directly from the index source map by future event-extraction
 * work.
 */
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
