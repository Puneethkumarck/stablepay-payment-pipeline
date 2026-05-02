package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record PaginatedResponse<T>(
    List<T> items,
    @JsonProperty("next_cursor") Optional<String> nextCursor,
    @JsonProperty("has_more") boolean hasMore) {

  public PaginatedResponse {
    Objects.requireNonNull(items, "items");
    Objects.requireNonNull(nextCursor, "nextCursor");
    items = List.copyOf(items);
  }
}
