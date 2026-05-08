package io.stablepay.api.domain.agent;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record TimelineEntry(
    String eventId, String source, String status, String detail, Instant timestamp) {

  public TimelineEntry {
    Objects.requireNonNull(eventId);
    Objects.requireNonNull(source);
    Objects.requireNonNull(status);
    Objects.requireNonNull(detail);
    Objects.requireNonNull(timestamp);
  }
}
