package io.stablepay.api.infrastructure.ratelimit;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.infrastructure.ratelimit.fixtures.RateLimitFixtures.productionConfigurationsForAllRoles;
import static io.stablepay.api.infrastructure.security.fixtures.JwtFixtures.jwtBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.stablepay.api.application.security.Role;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.infrastructure.security.AuthenticatedUserToken;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@Tag("integration")
class RateLimitFilterIT {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");
  private static final String SOME_ROUTE = "/api/v1/transactions";
  private static final int REDIS_PORT = 6379;

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:8.0-alpine"))
          .withExposedPorts(REDIS_PORT);

  private static RedisClient redisClient;
  private static StatefulRedisConnection<String, byte[]> connection;
  private static ProxyManager<String> proxyManager;
  private static Map<Role, BucketConfiguration> roleConfigurations;
  private static ObjectMapper objectMapper;

  @BeforeAll
  static void setUp() {
    var redisUri = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(REDIS_PORT);
    redisClient = RedisClient.create(redisUri);
    connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    proxyManager = Bucket4jLettuce.casBasedBuilder(connection).build();
    roleConfigurations = productionConfigurationsForAllRoles();
    objectMapper = JsonMapper.builder().findAndAddModules().build();
  }

  @AfterAll
  static void tearDown() {
    if (connection != null) {
      connection.close();
    }
    if (redisClient != null) {
      redisClient.shutdown();
    }
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAllow100CustomerRequestsThenReturn429() throws Exception {
    // given
    var filter = newFilter();
    authenticateAsCustomer();
    var chainInvocations = new AtomicInteger(0);
    FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();

    // when — fire 100 successful requests, then a 101st that must be rejected
    for (var i = 0; i < RateLimitConfig.CUSTOMER_LIMIT_PER_MINUTE; i++) {
      filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    }
    var rejected = new MockHttpServletResponse();
    filter.doFilter(getRequest(SOME_ROUTE), rejected, chain);

    // then
    assertThat(chainInvocations.get()).isEqualTo((int) RateLimitConfig.CUSTOMER_LIMIT_PER_MINUTE);
    assertThat(rejected.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(Long.parseLong(rejected.getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1L, 60L);
    var expectedBody = new ApiError("STBLPAY-4001", "Rate limit exceeded", FIXED_NOW);
    var actualBody = objectMapper.readValue(rejected.getContentAsString(), ApiError.class);
    assertThat(actualBody).usingRecursiveComparison().isEqualTo(expectedBody);
  }

  @Test
  void shouldShareBucketAcrossFilterInstances() throws Exception {
    // given — two filter instances backed by the same Redis-backed ProxyManager
    var firstInstance = newFilter();
    var secondInstance = newFilter();
    authenticateAsCustomer();
    var chainInvocations = new AtomicInteger(0);
    FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();
    var crossInstanceRoute = "/api/v1/cross-instance-route";

    // when — drain the bucket via the first instance, then attempt via the second
    for (var i = 0; i < RateLimitConfig.CUSTOMER_LIMIT_PER_MINUTE; i++) {
      firstInstance.doFilter(getRequest(crossInstanceRoute), new MockHttpServletResponse(), chain);
    }
    var rejected = new MockHttpServletResponse();
    secondInstance.doFilter(getRequest(crossInstanceRoute), rejected, chain);

    // then
    assertThat(chainInvocations.get()).isEqualTo((int) RateLimitConfig.CUSTOMER_LIMIT_PER_MINUTE);
    assertThat(rejected.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
  }

  private static RateLimitFilter newFilter() {
    var resolver =
        (RateLimitBucketResolver)
            (key, role) -> {
              var configuration = roleConfigurations.get(role);
              return proxyManager.builder().build(key, () -> configuration);
            };
    return new RateLimitFilter(resolver, objectMapper, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
  }

  private static MockHttpServletRequest getRequest(String uri) {
    var request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setRequestURI(uri);
    return request;
  }

  private static void authenticateAsCustomer() {
    var user = someCustomerUser();
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", user.email())
            .claim("roles", List.of("CUSTOMER"))
            .build();
    var token =
        new AuthenticatedUserToken(jwt, user, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    SecurityContextHolder.getContext().setAuthentication(token);
  }
}
