package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Schema(description = "DLQ replay operation result")
@Builder(toBuilder = true)
public record DlqReplayResponse(
    @JsonProperty("dlq_id") String dlqId, String status, Instant timestamp) {

  public DlqReplayResponse {
    Objects.requireNonNull(dlqId, "dlqId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(timestamp, "timestamp");
  }
}
