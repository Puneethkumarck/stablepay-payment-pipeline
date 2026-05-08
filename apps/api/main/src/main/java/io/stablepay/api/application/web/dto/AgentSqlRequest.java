package io.stablepay.api.application.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

public record AgentSqlRequest(@NotBlank String sql, Optional<@Min(1) @Max(10_000) Integer> limit) {

  public AgentSqlRequest {
    if (limit == null) {
      limit = Optional.empty();
    }
  }
}
