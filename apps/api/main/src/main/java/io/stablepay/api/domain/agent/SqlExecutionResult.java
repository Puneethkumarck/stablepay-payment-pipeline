package io.stablepay.api.domain.agent;

import java.util.List;
import java.util.Objects;
import lombok.Builder;

public sealed interface SqlExecutionResult
    permits SqlExecutionResult.Success, SqlExecutionResult.Rejected, SqlExecutionResult.Failed {

  @Builder(toBuilder = true)
  record Success(
      List<String> columns, List<List<Object>> rows, int rowCount, String queryId, long tookMs)
      implements SqlExecutionResult {
    public Success {
      Objects.requireNonNull(columns);
      Objects.requireNonNull(rows);
      Objects.requireNonNull(queryId);
      columns = List.copyOf(columns);
      rows = List.copyOf(rows);
    }
  }

  @Builder(toBuilder = true)
  record Rejected(String errorCode, String reason) implements SqlExecutionResult {
    public Rejected {
      Objects.requireNonNull(errorCode);
      Objects.requireNonNull(reason);
    }
  }

  @Builder(toBuilder = true)
  record Failed(String errorCode, String reason) implements SqlExecutionResult {
    public Failed {
      Objects.requireNonNull(errorCode);
      Objects.requireNonNull(reason);
    }
  }
}
