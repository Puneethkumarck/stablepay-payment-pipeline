package io.stablepay.api.infrastructure.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.infrastructure.security.AuthenticatedUser;
import io.stablepay.api.infrastructure.security.AuthenticatedUserToken;
import io.stablepay.api.infrastructure.security.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

  public static final String RATE_LIMIT_ERROR_CODE = "STBLPAY-4001";
  public static final String RATE_LIMIT_MESSAGE = "Rate limit exceeded";
  public static final String BUCKET_KEY_PREFIX = "rate:";
  static final long MIN_RETRY_AFTER_SECONDS = 1L;

  private final RateLimitBucketResolver bucketResolver;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var token = currentToken();
    if (token == null) {
      chain.doFilter(request, response);
      return;
    }
    var user = token.getPrincipal();
    var role = primaryRole(user);
    if (role == null) {
      chain.doFilter(request, response);
      return;
    }
    var key = bucketKey(user, request);
    var bucket = bucketResolver.resolve(key, role);
    var probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      chain.doFilter(request, response);
      return;
    }
    writeRateLimited(response, retryAfterSeconds(probe));
  }

  private static AuthenticatedUserToken currentToken() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication instanceof AuthenticatedUserToken token ? token : null;
  }

  static Role primaryRole(AuthenticatedUser user) {
    if (user.isAgent()) {
      return Role.AGENT;
    }
    if (user.isAdmin()) {
      return Role.ADMIN;
    }
    if (user.isCustomer()) {
      return Role.CUSTOMER;
    }
    return null;
  }

  static String bucketKey(AuthenticatedUser user, HttpServletRequest request) {
    return BUCKET_KEY_PREFIX
        + user.userId().value()
        + ":"
        + request.getMethod()
        + " "
        + normalizedRoute(request);
  }

  private static String normalizedRoute(HttpServletRequest request) {
    var uri = request.getRequestURI();
    var semi = uri.indexOf(';');
    if (semi >= 0) {
      uri = uri.substring(0, semi);
    }
    if (uri.length() > 1 && uri.endsWith("/")) {
      uri = uri.substring(0, uri.length() - 1);
    }
    return uri;
  }

  static long retryAfterSeconds(ConsumptionProbe probe) {
    var nanos = probe.getNanosToWaitForRefill();
    var seconds = (nanos + 999_999_999L) / 1_000_000_000L;
    return Math.max(MIN_RETRY_AFTER_SECONDS, seconds);
  }

  private void writeRateLimited(HttpServletResponse response, long retryAfterSeconds)
      throws IOException {
    var body = new ApiError(RATE_LIMIT_ERROR_CODE, RATE_LIMIT_MESSAGE, clock.instant());
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
