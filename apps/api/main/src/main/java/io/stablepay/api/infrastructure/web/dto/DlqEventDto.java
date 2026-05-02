package io.stablepay.api.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record DlqEventDto(
    String id,
    @JsonProperty("error_class") String errorClass,
    @JsonProperty("source_topic") String sourceTopic,
    @JsonProperty("source_partition") int sourcePartition,
    @JsonProperty("source_offset") long sourceOffset,
    @JsonProperty("error_message") String errorMessage,
    @JsonProperty("failed_at") Instant failedAt,
    @JsonProperty("retry_count") int retryCount,
    @JsonProperty("sink_type") Optional<String> sinkType,
    @JsonProperty("watermark_at") Optional<Instant> watermarkAt,
    @JsonProperty("original_payload_json") Optional<String> originalPayloadJson) {

  public DlqEventDto {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(errorClass, "errorClass");
    Objects.requireNonNull(sourceTopic, "sourceTopic");
    Objects.requireNonNull(errorMessage, "errorMessage");
    Objects.requireNonNull(failedAt, "failedAt");
    Objects.requireNonNull(sinkType, "sinkType");
    Objects.requireNonNull(watermarkAt, "watermarkAt");
    Objects.requireNonNull(originalPayloadJson, "originalPayloadJson");
  }
}
