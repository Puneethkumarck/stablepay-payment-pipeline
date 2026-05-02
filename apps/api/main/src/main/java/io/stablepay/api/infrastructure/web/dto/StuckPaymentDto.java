package io.stablepay.api.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record StuckPaymentDto(
    String id,
    String reference,
    @JsonProperty("flow_type") String flowType,
    @JsonProperty("internal_status") String internalStatus,
    @JsonProperty("customer_id") String customerId,
    AmountDto amount,
    @JsonProperty("last_event_at") Instant lastEventAt,
    @JsonProperty("stuck_millis") long stuckMillis) {

  public StuckPaymentDto {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(internalStatus, "internalStatus");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(lastEventAt, "lastEventAt");
  }
}
