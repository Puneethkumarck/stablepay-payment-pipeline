package io.stablepay.api.domain.port;

import io.stablepay.api.domain.agent.AgentSearchRequest;
import io.stablepay.api.domain.agent.SearchExecutionResult;

public interface AgentSearchExecutor {

  SearchExecutionResult executeSearch(AgentSearchRequest request, int resolvedSize);
}
