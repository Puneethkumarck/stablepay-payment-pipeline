package io.stablepay.api.infrastructure.ratelimit.fixtures;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.stablepay.api.application.security.Role;
import io.stablepay.api.infrastructure.ratelimit.RateLimitBucketResolver;
import io.stablepay.api.infrastructure.ratelimit.RateLimitConfig;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimitFixtures {

  public static final long SOME_TINY_CAPACITY = 3L;
  public static final Duration SOME_LIMIT_WINDOW = Duration.ofMinutes(1);

  private RateLimitFixtures() {}

  public static Map<Role, BucketConfiguration> tinyConfigurationsForAllRoles() {
    var configurations = new EnumMap<Role, BucketConfiguration>(Role.class);
    var configuration =
        BucketConfiguration.builder()
            .addLimit(
                limit ->
                    limit
                        .capacity(SOME_TINY_CAPACITY)
                        .refillGreedy(SOME_TINY_CAPACITY, SOME_LIMIT_WINDOW))
            .build();
    configurations.put(Role.CUSTOMER, configuration);
    configurations.put(Role.ADMIN, configuration);
    configurations.put(Role.AGENT, configuration);
    return Map.copyOf(configurations);
  }

  public static Map<Role, BucketConfiguration> productionConfigurationsForAllRoles() {
    var configurations = new EnumMap<Role, BucketConfiguration>(Role.class);
    configurations.put(
        Role.CUSTOMER,
        BucketConfiguration.builder()
            .addLimit(
                limit ->
                    limit
                        .capacity(RateLimitConfig.CUSTOMER_LIMIT_PER_MINUTE)
                        .refillGreedy(RateLimitConfig.CUSTOMER_LIMIT_PER_MINUTE, SOME_LIMIT_WINDOW))
            .build());
    configurations.put(
        Role.ADMIN,
        BucketConfiguration.builder()
            .addLimit(
                limit ->
                    limit
                        .capacity(RateLimitConfig.ADMIN_LIMIT_PER_MINUTE)
                        .refillGreedy(RateLimitConfig.ADMIN_LIMIT_PER_MINUTE, SOME_LIMIT_WINDOW))
            .build());
    configurations.put(
        Role.AGENT,
        BucketConfiguration.builder()
            .addLimit(
                limit ->
                    limit
                        .capacity(RateLimitConfig.AGENT_LIMIT_PER_MINUTE)
                        .refillGreedy(RateLimitConfig.AGENT_LIMIT_PER_MINUTE, SOME_LIMIT_WINDOW))
            .build());
    return Map.copyOf(configurations);
  }

  public static InMemoryBucketResolver inMemoryResolverWith(
      Map<Role, BucketConfiguration> configurations) {
    return new InMemoryBucketResolver(configurations);
  }

  public static final class InMemoryBucketResolver implements RateLimitBucketResolver {

    private final Map<Role, BucketConfiguration> configurations;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public InMemoryBucketResolver(Map<Role, BucketConfiguration> configurations) {
      this.configurations = configurations;
    }

    @Override
    public Bucket resolve(String key, Role role) {
      return buckets.computeIfAbsent(key, ignored -> newLocalBucket(configurations.get(role)));
    }

    private static Bucket newLocalBucket(BucketConfiguration configuration) {
      var builder = Bucket.builder();
      for (var bandwidth : configuration.getBandwidths()) {
        builder.addLimit(bandwidth);
      }
      return builder.build();
    }
  }
}
