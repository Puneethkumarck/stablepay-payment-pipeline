package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransactionEventDto(
    @JsonProperty("event_id") String eventId,
    @JsonProperty("customer_id") String customerId,
    String status,
    @JsonProperty("event_time") Instant eventTime,
    @JsonProperty("amount_micros") long amountMicros,
    @JsonProperty("currency_code") String currencyCode,
    @JsonProperty("flow_type") String flowType,
    @JsonProperty("sort_key") String sortKey) {

  public TransactionEventDto {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(eventTime, "eventTime");
    Objects.requireNonNull(currencyCode, "currencyCode");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(sortKey, "sortKey");
  }
}
