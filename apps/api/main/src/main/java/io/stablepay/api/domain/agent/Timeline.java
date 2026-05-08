package io.stablepay.api.domain.agent;

import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record Timeline(String reference, String markdown, List<TimelineEntry> entries) {

  public Timeline {
    Objects.requireNonNull(reference);
    Objects.requireNonNull(markdown);
    Objects.requireNonNull(entries);
    entries = List.copyOf(entries);
  }
}
