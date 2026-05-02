package io.stablepay.api.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record FlowDto(
    String id,
    @JsonProperty("flow_type") String flowType,
    String status,
    @JsonProperty("customer_id") String customerId,
    @JsonProperty("total_amount") AmountDto totalAmount,
    @JsonProperty("leg_count") int legCount,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt,
    @JsonProperty("completed_at") Optional<Instant> completedAt) {

  public FlowDto {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(flowType, "flowType");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(totalAmount, "totalAmount");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    Objects.requireNonNull(completedAt, "completedAt");
  }
}
