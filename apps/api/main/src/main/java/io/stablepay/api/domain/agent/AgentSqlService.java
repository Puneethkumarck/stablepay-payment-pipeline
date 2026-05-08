package io.stablepay.api.domain.agent;

import io.stablepay.api.domain.port.AgentSqlExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentSqlService {

  private final TrinoSqlAllowlistValidator validator;
  private final AgentSqlExecutor executor;

  public SqlExecutionResult execute(String rawSql, int requestedLimit) {
    var validation = validator.validate(rawSql, requestedLimit);
    return switch (validation) {
      case SqlValidationResult.Invalid invalid ->
          new SqlExecutionResult.Rejected(invalid.errorCode(), invalid.reason());
      case SqlValidationResult.Valid valid ->
          executor.executeQuery(valid.sanitizedSql(), valid.appliedLimit());
    };
  }
}
