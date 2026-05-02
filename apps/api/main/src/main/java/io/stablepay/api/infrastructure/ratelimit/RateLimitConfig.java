package io.stablepay.api.infrastructure.ratelimit;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.stablepay.api.infrastructure.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
@Slf4j
public class RateLimitConfig {

  public static final long CUSTOMER_LIMIT_PER_MINUTE = 100L;
  public static final long ADMIN_LIMIT_PER_MINUTE = 500L;
  public static final long AGENT_LIMIT_PER_MINUTE = 1_000L;
  public static final Duration LIMIT_WINDOW = Duration.ofMinutes(1);
  public static final Duration BUCKET_EXPIRATION = Duration.ofMinutes(5);

  private final RateLimitProperties properties;

  @Bean(destroyMethod = "shutdown")
  public RedisClient rateLimitRedisClient() {
    log.info("Configured Bucket4j Redis client for {}", properties.redisUri());
    return RedisClient.create(properties.redisUri());
  }

  @Bean(destroyMethod = "close")
  public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(
      RedisClient rateLimitRedisClient) {
    return rateLimitRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
  }

  @Bean
  public ProxyManager<String> rateLimitProxyManager(
      StatefulRedisConnection<String, byte[]> rateLimitRedisConnection) {
    return Bucket4jLettuce.casBasedBuilder(rateLimitRedisConnection)
        .expirationAfterWrite(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(BUCKET_EXPIRATION))
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public Map<Role, BucketConfiguration> roleBucketConfigurations() {
    var configurations = new EnumMap<Role, BucketConfiguration>(Role.class);
    configurations.put(Role.CUSTOMER, configurationFor(CUSTOMER_LIMIT_PER_MINUTE));
    configurations.put(Role.ADMIN, configurationFor(ADMIN_LIMIT_PER_MINUTE));
    configurations.put(Role.AGENT, configurationFor(AGENT_LIMIT_PER_MINUTE));
    return Map.copyOf(configurations);
  }

  @Bean
  public RateLimitBucketResolver rateLimitBucketResolver(
      ProxyManager<String> rateLimitProxyManager,
      Map<Role, BucketConfiguration> roleBucketConfigurations) {
    return (key, role) -> {
      var configuration = roleBucketConfigurations.get(role);
      return rateLimitProxyManager.builder().build(key, () -> configuration);
    };
  }

  static BucketConfiguration configurationFor(long capacity) {
    return BucketConfiguration.builder()
        .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, LIMIT_WINDOW))
        .build();
  }
}
