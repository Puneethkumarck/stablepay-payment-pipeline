package io.stablepay.api.domain.agent;

import java.util.Objects;
import lombok.Builder;

public sealed interface SqlValidationResult
    permits SqlValidationResult.Valid, SqlValidationResult.Invalid {

  @Builder(toBuilder = true)
  record Valid(String sanitizedSql, int appliedLimit) implements SqlValidationResult {
    public Valid {
      Objects.requireNonNull(sanitizedSql);
    }
  }

  @Builder(toBuilder = true)
  record Invalid(String reason, String errorCode) implements SqlValidationResult {
    public Invalid {
      Objects.requireNonNull(reason);
      Objects.requireNonNull(errorCode);
    }
  }
}
