package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record DlqSummaryDto(
    @JsonProperty("counts_by_error_class") Map<String, Long> countsByErrorClass,
    @JsonProperty("total_count") long totalCount) {

  public DlqSummaryDto {
    Objects.requireNonNull(countsByErrorClass, "countsByErrorClass");
    countsByErrorClass = Map.copyOf(countsByErrorClass);
  }
}
