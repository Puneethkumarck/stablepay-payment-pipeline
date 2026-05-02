package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record DlqReplayResponse(
    @JsonProperty("dlq_id") String dlqId, String status, Instant timestamp) {

  public DlqReplayResponse {
    Objects.requireNonNull(dlqId, "dlqId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(timestamp, "timestamp");
  }
}
