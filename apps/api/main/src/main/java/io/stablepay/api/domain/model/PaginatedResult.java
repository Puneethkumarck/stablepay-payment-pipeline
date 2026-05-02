package io.stablepay.api.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record PaginatedResult<T>(List<T> items, Optional<String> nextCursor) {

  public PaginatedResult {
    Objects.requireNonNull(items, "items");
    Objects.requireNonNull(nextCursor, "nextCursor");
    items = List.copyOf(items);
  }
}
