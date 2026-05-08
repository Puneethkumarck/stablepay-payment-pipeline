package io.stablepay.api.domain.agent;

import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public sealed interface DslValidationResult
    permits DslValidationResult.Valid, DslValidationResult.Invalid {

  @Builder(toBuilder = true)
  record Valid(Query translatedQuery, Map<String, Aggregation> translatedAggs, int size)
      implements DslValidationResult {
    public Valid {
      Objects.requireNonNull(translatedQuery);
      Objects.requireNonNull(translatedAggs);
    }
  }

  @Builder(toBuilder = true)
  record Invalid(String reason, String errorCode) implements DslValidationResult {
    public Invalid {
      Objects.requireNonNull(reason);
      Objects.requireNonNull(errorCode);
    }
  }
}
