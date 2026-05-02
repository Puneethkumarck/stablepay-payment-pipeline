package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record DlqEvent(
    DlqId id,
    String errorClass,
    String sourceTopic,
    int sourcePartition,
    long sourceOffset,
    String errorMessage,
    Instant failedAt,
    int retryCount,
    Optional<String> sinkType,
    Optional<Instant> watermarkAt,
    Optional<String> originalPayloadJson) {

  public DlqEvent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(errorClass, "errorClass");
    Objects.requireNonNull(sourceTopic, "sourceTopic");
    Objects.requireNonNull(errorMessage, "errorMessage");
    Objects.requireNonNull(failedAt, "failedAt");
    Objects.requireNonNull(sinkType, "sinkType");
    Objects.requireNonNull(watermarkAt, "watermarkAt");
    Objects.requireNonNull(originalPayloadJson, "originalPayloadJson");
    if (sourcePartition < 0) {
      throw new IllegalArgumentException("sourcePartition must be >= 0");
    }
    if (sourceOffset < 0) {
      throw new IllegalArgumentException("sourceOffset must be >= 0");
    }
    if (retryCount < 0) {
      throw new IllegalArgumentException("retryCount must be >= 0");
    }
  }
}
