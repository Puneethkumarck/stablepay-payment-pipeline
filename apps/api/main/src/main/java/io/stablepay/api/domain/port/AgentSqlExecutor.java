package io.stablepay.api.domain.port;

import io.stablepay.api.domain.agent.SqlExecutionResult;

public interface AgentSqlExecutor {

  SqlExecutionResult executeQuery(String sanitizedSql, int appliedLimit);
}
