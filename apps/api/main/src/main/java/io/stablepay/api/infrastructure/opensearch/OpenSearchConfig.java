package io.stablepay.api.infrastructure.opensearch;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO(SPP-NN): switch to authenticated HTTPS transport (basic auth + TLS truststore) before any
// non-localhost deployment. Bare HTTP is acceptable only for the local docker-compose target.
@Configuration
@Slf4j
public class OpenSearchConfig {

  @Bean
  public OpenSearchClient openSearchClient(@Value("${stablepay.opensearch.uri}") String uri) {
    log.info("Configured OpenSearch client for {}", uri);
    var transport =
        ApacheHttpClient5TransportBuilder.builder(HttpHost.create(URI.create(uri)))
            .setMapper(new JacksonJsonpMapper())
            .build();
    return new OpenSearchClient(transport);
  }
}
