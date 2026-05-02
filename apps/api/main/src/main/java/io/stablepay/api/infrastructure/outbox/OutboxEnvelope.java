package io.stablepay.api.infrastructure.outbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record OutboxEnvelope(
    @JsonProperty("topic") String topic, @JsonProperty("payload_b64") String payloadBase64) {

  public OutboxEnvelope {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(payloadBase64, "payloadBase64");
  }

  public static OutboxEnvelope of(String topic, byte[] payload) {
    Objects.requireNonNull(payload, "payload");
    return new OutboxEnvelope(topic, Base64.getEncoder().encodeToString(payload));
  }
}
