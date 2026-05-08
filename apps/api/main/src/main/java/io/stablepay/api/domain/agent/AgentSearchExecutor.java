package io.stablepay.api.domain.agent;

public interface AgentSearchExecutor {

  SearchExecutionResult executeSearch(AgentSearchRequest request, int resolvedSize);
}
