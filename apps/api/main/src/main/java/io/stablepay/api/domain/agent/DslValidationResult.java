package io.stablepay.api.domain.agent;

import java.util.Objects;
import lombok.Builder;

public sealed interface DslValidationResult
    permits DslValidationResult.Valid, DslValidationResult.Invalid {

  @Builder(toBuilder = true)
  record Valid(AgentSearchRequest validatedRequest, int resolvedSize)
      implements DslValidationResult {
    public Valid {
      Objects.requireNonNull(validatedRequest);
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
