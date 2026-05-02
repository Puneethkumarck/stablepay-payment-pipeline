package io.stablepay.api.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.stablepay.api.application.security.Role;
import org.junit.jupiter.api.Test;

class RateLimitConfigTest {

  private final RateLimitConfig config =
      new RateLimitConfig(RateLimitProperties.builder().redisUri("redis://localhost:6379").build());

  @Test
  void shouldGiveCustomerOneHundredTokensPerMinute() {
    // given
    var configuration = config.roleBucketConfigurations().get(Role.CUSTOMER);

    // when
    var consumed = drainBucket(configuration);

    // then
    assertThat(consumed).isEqualTo(RateLimitConfig.CUSTOMER_LIMIT_PER_MINUTE);
  }

  @Test
  void shouldGiveAdminFiveHundredTokensPerMinute() {
    // given
    var configuration = config.roleBucketConfigurations().get(Role.ADMIN);

    // when
    var consumed = drainBucket(configuration);

    // then
    assertThat(consumed).isEqualTo(RateLimitConfig.ADMIN_LIMIT_PER_MINUTE);
  }

  @Test
  void shouldGiveAgentOneThousandTokensPerMinute() {
    // given
    var configuration = config.roleBucketConfigurations().get(Role.AGENT);

    // when
    var consumed = drainBucket(configuration);

    // then
    assertThat(consumed).isEqualTo(RateLimitConfig.AGENT_LIMIT_PER_MINUTE);
  }

  @Test
  void shouldExposeAllThreeRolesInConfigurationsMap() {
    // when
    var configurations = config.roleBucketConfigurations();

    // then
    assertThat(configurations).containsOnlyKeys(Role.CUSTOMER, Role.ADMIN, Role.AGENT);
  }

  private static long drainBucket(BucketConfiguration configuration) {
    var builder = Bucket.builder();
    for (var bandwidth : configuration.getBandwidths()) {
      builder.addLimit(bandwidth);
    }
    var bucket = builder.build();
    var consumed = 0L;
    while (bucket.tryConsume(1)) {
      consumed++;
    }
    return consumed;
  }
}
