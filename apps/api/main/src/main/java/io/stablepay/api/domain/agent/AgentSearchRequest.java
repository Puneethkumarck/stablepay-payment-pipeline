package io.stablepay.api.domain.agent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record AgentSearchRequest(
    String index,
    QueryShape query,
    Optional<List<AggShape>> aggs,
    Optional<Integer> size,
    Optional<List<String>> sort) {

  public AgentSearchRequest {
    Objects.requireNonNull(index);
    Objects.requireNonNull(query);
    Objects.requireNonNull(aggs);
    Objects.requireNonNull(size);
    Objects.requireNonNull(sort);
  }
}
