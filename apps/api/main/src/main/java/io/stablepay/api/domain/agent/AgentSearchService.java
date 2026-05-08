package io.stablepay.api.domain.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentSearchService {

  private final OpenSearchDslWhitelistValidator validator;
  private final AgentSearchExecutor executor;

  public SearchExecutionResult execute(AgentSearchRequest request) {
    var validation = validator.validate(request);
    return switch (validation) {
      case DslValidationResult.Invalid invalid ->
          new SearchExecutionResult.Rejected(invalid.errorCode(), invalid.reason());
      case DslValidationResult.Valid valid ->
          executor.executeSearch(valid.validatedRequest(), valid.resolvedSize());
    };
  }
}
