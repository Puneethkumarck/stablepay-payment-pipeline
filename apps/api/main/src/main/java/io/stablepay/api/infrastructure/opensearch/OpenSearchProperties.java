package io.stablepay.api.infrastructure.opensearch;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder(toBuilder = true)
@ConfigurationProperties("stablepay.opensearch")
public record OpenSearchProperties(String uri, String transactionsIndex) {

  public OpenSearchProperties {
    if (transactionsIndex == null || transactionsIndex.isBlank()) {
      transactionsIndex = "transactions";
    }
  }
}
