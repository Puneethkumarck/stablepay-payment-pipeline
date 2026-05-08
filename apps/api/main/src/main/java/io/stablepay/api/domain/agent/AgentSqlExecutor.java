package io.stablepay.api.domain.agent;

public interface AgentSqlExecutor {

  SqlExecutionResult executeQuery(String sanitizedSql, int appliedLimit);
}
