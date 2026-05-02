package io.stablepay.api.infrastructure.ratelimit;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder(toBuilder = true)
@ConfigurationProperties("stablepay.ratelimit")
public record RateLimitProperties(String redisUri) {

  public RateLimitProperties {
    if (redisUri == null || redisUri.isBlank()) {
      redisUri = "redis://redis:6379";
    }
  }
}
