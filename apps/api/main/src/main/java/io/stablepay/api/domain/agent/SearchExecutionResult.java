package io.stablepay.api.domain.agent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

public sealed interface SearchExecutionResult
    permits SearchExecutionResult.Success,
        SearchExecutionResult.Rejected,
        SearchExecutionResult.Failed {

  @Builder(toBuilder = true)
  record Success(List<Map<String, Object>> hits, long totalHits, int resolvedSize, long tookMs)
      implements SearchExecutionResult {
    public Success {
      Objects.requireNonNull(hits);
      hits = List.copyOf(hits);
    }
  }

  @Builder(toBuilder = true)
  record Rejected(String errorCode, String reason) implements SearchExecutionResult {
    public Rejected {
      Objects.requireNonNull(errorCode);
      Objects.requireNonNull(reason);
    }
  }

  @Builder(toBuilder = true)
  record Failed(String errorCode, String reason) implements SearchExecutionResult {
    public Failed {
      Objects.requireNonNull(errorCode);
      Objects.requireNonNull(reason);
    }
  }
}
