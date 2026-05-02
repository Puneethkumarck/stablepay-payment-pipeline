package io.stablepay.api.domain.model;

import java.util.Map;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record DlqSummary(Map<String, Long> countsByErrorClass, long totalCount) {

  public DlqSummary {
    Objects.requireNonNull(countsByErrorClass, "countsByErrorClass");
    countsByErrorClass = Map.copyOf(countsByErrorClass);
  }
}
