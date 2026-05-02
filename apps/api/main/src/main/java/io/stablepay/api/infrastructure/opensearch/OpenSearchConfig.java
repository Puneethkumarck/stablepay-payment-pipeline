package io.stablepay.api.infrastructure.opensearch;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO(SPP-NN): switch to authenticated HTTPS transport (basic auth + TLS truststore) before any
// non-localhost deployment. Bare HTTP is acceptable only for the local docker-compose target.
@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
@RequiredArgsConstructor
@Slf4j
public class OpenSearchConfig {

  private final OpenSearchProperties properties;

  @Bean
  public OpenSearchClient openSearchClient() {
    log.info("Configured OpenSearch client for {}", properties.uri());
    var transport =
        ApacheHttpClient5TransportBuilder.builder(HttpHost.create(URI.create(properties.uri())))
            .setMapper(new JacksonJsonpMapper())
            .build();
    return new OpenSearchClient(transport);
  }
}
