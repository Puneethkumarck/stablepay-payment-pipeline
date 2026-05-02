package io.stablepay.api.infrastructure.ratelimit;

import static io.stablepay.api.infrastructure.ratelimit.fixtures.RateLimitFixtures.SOME_TINY_CAPACITY;
import static io.stablepay.api.infrastructure.ratelimit.fixtures.RateLimitFixtures.inMemoryResolverWith;
import static io.stablepay.api.infrastructure.ratelimit.fixtures.RateLimitFixtures.tinyConfigurationsForAllRoles;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_AGENT_USER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_USER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAgentUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.infrastructure.security.fixtures.JwtFixtures.jwtBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.client.ApiError;
import io.stablepay.api.infrastructure.security.AuthenticatedUser;
import io.stablepay.api.infrastructure.security.AuthenticatedUserToken;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");
  private static final String SOME_ROUTE = "/api/v1/transactions";

  private RateLimitFilter filter;
  private ObjectMapper objectMapper;
  private AtomicInteger chainInvocations;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    objectMapper = JsonMapper.builder().findAndAddModules().build();
    filter =
        new RateLimitFilter(
            inMemoryResolverWith(tinyConfigurationsForAllRoles()),
            objectMapper,
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    chainInvocations = new AtomicInteger(0);
    chain = (req, res) -> chainInvocations.incrementAndGet();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldPassThroughWhenNoAuthentication() throws Exception {
    // given
    var request = getRequest(SOME_ROUTE);

    // when
    filter.doFilter(request, new MockHttpServletResponse(), chain);

    // then
    assertThat(chainInvocations.get()).isEqualTo(1);
  }

  @Test
  void shouldAllowRequestsUpToBucketCapacity() throws Exception {
    // given
    authenticateAs(someCustomerUser(), "CUSTOMER");

    // when
    for (var i = 0; i < SOME_TINY_CAPACITY; i++) {
      filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    }

    // then
    assertThat(chainInvocations.get()).isEqualTo((int) SOME_TINY_CAPACITY);
  }

  @Test
  void shouldRejectRequestAfterCapacityWith429RetryAfterAndApiError() throws Exception {
    // given
    authenticateAs(someCustomerUser(), "CUSTOMER");
    for (var i = 0; i < SOME_TINY_CAPACITY; i++) {
      filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    }
    var response = new MockHttpServletResponse();
    var expectedBody = new ApiError("STBLPAY-4001", "Rate limit exceeded", FIXED_NOW);

    // when
    filter.doFilter(getRequest(SOME_ROUTE), response, chain);

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(Long.parseLong(response.getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1L, 60L);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    var actualBody = objectMapper.readValue(response.getContentAsString(), ApiError.class);
    assertThat(actualBody).usingRecursiveComparison().isEqualTo(expectedBody);
    assertThat(chainInvocations.get()).isEqualTo((int) SOME_TINY_CAPACITY);
  }

  @Test
  void shouldUseAdminBucketForAdminPrincipal() throws Exception {
    // given
    authenticateAs(someAdminUser(), "ADMIN");

    // when
    for (var i = 0; i < SOME_TINY_CAPACITY + 1; i++) {
      filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    }

    // then — admin shares the same tiny capacity in this test fixture
    assertThat(chainInvocations.get()).isEqualTo((int) SOME_TINY_CAPACITY);
  }

  @Test
  void shouldUseAgentBucketForAgentPrincipal() throws Exception {
    // given
    authenticateAs(someAgentUser(), "AGENT");

    // when
    for (var i = 0; i < SOME_TINY_CAPACITY + 1; i++) {
      filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    }

    // then
    assertThat(chainInvocations.get()).isEqualTo((int) SOME_TINY_CAPACITY);
  }

  @Test
  void shouldRateLimitPerUserIndependently() throws Exception {
    // given
    authenticateAs(someCustomerUser(), "CUSTOMER");
    for (var i = 0; i < SOME_TINY_CAPACITY; i++) {
      filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    }
    SecurityContextHolder.clearContext();

    // when — a different user should have a fresh bucket
    authenticateAs(someAdminUser(), "ADMIN");
    filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);

    // then
    assertThat(chainInvocations.get()).isEqualTo((int) SOME_TINY_CAPACITY + 1);
  }

  @Test
  void shouldRateLimitPerRouteIndependently() throws Exception {
    // given
    authenticateAs(someCustomerUser(), "CUSTOMER");
    for (var i = 0; i < SOME_TINY_CAPACITY; i++) {
      filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    }

    // when — different route gets a fresh bucket for the same user
    filter.doFilter(getRequest("/api/v1/dashboard/stats"), new MockHttpServletResponse(), chain);

    // then
    assertThat(chainInvocations.get()).isEqualTo((int) SOME_TINY_CAPACITY + 1);
  }

  @Test
  void shouldNormalizeTrailingSlashAndMatrixParamsForRoute() throws Exception {
    // given
    authenticateAs(someCustomerUser(), "CUSTOMER");

    // when — three normalized variants of the same route exhaust the bucket
    filter.doFilter(getRequest(SOME_ROUTE), new MockHttpServletResponse(), chain);
    filter.doFilter(getRequest(SOME_ROUTE + "/"), new MockHttpServletResponse(), chain);
    filter.doFilter(
        getRequest(SOME_ROUTE + ";jsessionid=abc"), new MockHttpServletResponse(), chain);
    var fourth = new MockHttpServletResponse();
    filter.doFilter(getRequest(SOME_ROUTE), fourth, chain);

    // then
    assertThat(chainInvocations.get()).isEqualTo((int) SOME_TINY_CAPACITY);
    assertThat(fourth.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
  }

  private static MockHttpServletRequest getRequest(String uri) {
    var request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setRequestURI(uri);
    return request;
  }

  private static void authenticateAs(AuthenticatedUser user, String role) {
    var jwt =
        jwtBuilder()
            .subject(subjectFor(user))
            .claim("email", user.email())
            .claim("roles", List.of(role))
            .build();
    var token =
        new AuthenticatedUserToken(jwt, user, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  private static String subjectFor(AuthenticatedUser user) {
    if (user.isAgent()) {
      return SOME_AGENT_USER_UUID.toString();
    }
    if (user.isAdmin()) {
      return SOME_ADMIN_USER_UUID.toString();
    }
    return SOME_CUSTOMER_USER_UUID.toString();
  }
}
