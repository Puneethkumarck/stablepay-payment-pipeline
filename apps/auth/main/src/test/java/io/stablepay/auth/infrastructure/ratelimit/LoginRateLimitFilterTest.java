package io.stablepay.auth.infrastructure.ratelimit;

import static org.mockito.BDDMockito.willAnswer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.auth.client.ApiError;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");
  private static final String LOGIN_PATH = "/api/v1/auth/login";
  private static final String SOME_IP = "203.0.113.7";

  @Mock private FilterChain chain;

  private LoginRateLimitFilter filter;
  private ObjectMapper objectMapper;
  private AtomicInteger chainInvocations;

  @BeforeEach
  void setUp() throws Exception {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    filter = new LoginRateLimitFilter(objectMapper, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    chainInvocations = new AtomicInteger(0);
    willAnswer(
            invocation -> {
              chainInvocations.incrementAndGet();
              return null;
            })
        .given(chain)
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldAllowFirstFiveLoginRequestsFromSameIp() throws Exception {
    // given
    // when
    for (var i = 0; i < 5; i++) {
      filter.doFilter(loginRequest(), new MockHttpServletResponse(), chain);
    }

    // then
    Assertions.assertThat(chainInvocations.get()).isEqualTo(5);
  }

  @Test
  void shouldReturn429WithRetryAfterAndApiErrorOnSixthRequest() throws Exception {
    // given — exhaust the bucket
    for (var i = 0; i < 5; i++) {
      filter.doFilter(loginRequest(), new MockHttpServletResponse(), chain);
    }
    var response = new MockHttpServletResponse();
    var expectedBody =
        new ApiError(
            "STBLPAY-1004", "Too many login attempts. Try again in 60 seconds.", FIXED_NOW);

    // when
    filter.doFilter(loginRequest(), response, chain);

    // then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    Assertions.assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
    Assertions.assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    var actualBody = objectMapper.readValue(response.getContentAsString(), ApiError.class);
    Assertions.assertThat(actualBody).usingRecursiveComparison().isEqualTo(expectedBody);
    Assertions.assertThat(chainInvocations.get()).isEqualTo(5);
  }

  @Test
  void shouldNotRateLimitNonLoginPaths() throws Exception {
    // given
    var request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setRequestURI("/api/v1/auth/refresh");
    request.setRemoteAddr(SOME_IP);

    // when — fire 10 times, well above the 5/min limit
    for (var i = 0; i < 10; i++) {
      filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    // then
    Assertions.assertThat(chainInvocations.get()).isEqualTo(10);
  }

  @Test
  void shouldRateLimitPerIpIndependently() throws Exception {
    // given — exhaust bucket for ip1
    for (var i = 0; i < 5; i++) {
      filter.doFilter(loginRequestFrom("198.51.100.1"), new MockHttpServletResponse(), chain);
    }

    // when — first request from a fresh ip2 must succeed
    var response = new MockHttpServletResponse();
    filter.doFilter(loginRequestFrom("198.51.100.2"), response, chain);

    // then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    Assertions.assertThat(chainInvocations.get()).isEqualTo(6);
  }

  private MockHttpServletRequest loginRequest() {
    return loginRequestFrom(SOME_IP);
  }

  private MockHttpServletRequest loginRequestFrom(String ip) {
    var request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setRequestURI(LOGIN_PATH);
    request.setRemoteAddr(ip);
    return request;
  }
}
