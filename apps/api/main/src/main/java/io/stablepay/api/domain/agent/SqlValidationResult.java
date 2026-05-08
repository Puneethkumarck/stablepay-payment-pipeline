package io.stablepay.api.domain.agent;

import lombok.Builder;

public sealed interface SqlValidationResult
    permits SqlValidationResult.Valid, SqlValidationResult.Invalid {

  @Builder(toBuilder = true)
  record Valid(String sanitizedSql, int appliedLimit) implements SqlValidationResult {}

  @Builder(toBuilder = true)
  record Invalid(String reason, String errorCode) implements SqlValidationResult {}
}
