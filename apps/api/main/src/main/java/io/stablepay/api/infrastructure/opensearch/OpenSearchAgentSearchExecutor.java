package io.stablepay.api.infrastructure.opensearch;

import io.stablepay.api.domain.agent.AgentSearchExecutor;
import io.stablepay.api.domain.agent.AgentSearchRequest;
import io.stablepay.api.domain.agent.SearchExecutionResult;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenSearchAgentSearchExecutor implements AgentSearchExecutor {

  static final String EXECUTION_FAILED_CODE = "STBLPAY-6006";

  private final OpenSearchQueryTranslator translator;
  private final OpenSearchClient openSearchClient;

  @Override
  @SuppressWarnings("unchecked")
  public SearchExecutionResult executeSearch(AgentSearchRequest request, int resolvedSize) {
    var startNanos = System.nanoTime();
    var osQuery = translator.translateQuery(request.query());
    var searchRequest =
        SearchRequest.of(
            r -> {
              r.index(request.index()).size(resolvedSize).query(osQuery);
              request.aggs().ifPresent(aggs -> r.aggregations(translator.translateAggs(aggs)));
              return r;
            });
    try {
      var response = openSearchClient.search(searchRequest, Map.class);
      var hits =
          response.hits().hits().stream()
              .map(Hit::source)
              .map(src -> (Map<String, Object>) src)
              .toList();
      var totalHits =
          response.hits().total() != null ? response.hits().total().value() : hits.size();
      var tookMs = (System.nanoTime() - startNanos) / 1_000_000;
      return new SearchExecutionResult.Success(hits, totalHits, resolvedSize, tookMs);
    } catch (IOException | OpenSearchException e) {
      log.warn("Agent search execution failed: index={}", request.index(), e);
      return new SearchExecutionResult.Failed(EXECUTION_FAILED_CODE, "Search execution failed");
    }
  }
}
