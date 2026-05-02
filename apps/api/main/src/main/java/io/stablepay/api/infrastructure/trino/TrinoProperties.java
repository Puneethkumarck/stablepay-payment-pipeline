package io.stablepay.api.infrastructure.trino;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder(toBuilder = true)
@ConfigurationProperties("stablepay.trino")
public record TrinoProperties(String url, String user) {

  public TrinoProperties {
    if (user == null || user.isBlank()) {
      user = "stablepay";
    }
  }
}
