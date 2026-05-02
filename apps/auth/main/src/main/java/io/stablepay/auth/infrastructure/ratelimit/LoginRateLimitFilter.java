package io.stablepay.auth.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.stablepay.auth.client.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN_PATH = "/api/v1/auth/login";
  private static final String RATE_LIMIT_ERROR_CODE = "STBLPAY-1004";
  private static final int BUCKET_CAPACITY = 5;
  private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
  private static final long RETRY_AFTER_SECONDS = REFILL_PERIOD.toSeconds();
  private static final String RATE_LIMIT_MESSAGE =
      "Too many login attempts. Try again in " + RETRY_AFTER_SECONDS + " seconds.";
  private static final Duration BUCKET_IDLE_TTL = Duration.ofMinutes(2);
  private static final long BUCKET_MAX_ENTRIES = 100_000L;

  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder()
          .expireAfterAccess(BUCKET_IDLE_TTL)
          .maximumSize(BUCKET_MAX_ENTRIES)
          .build();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!isLoginRequest(request)) {
      chain.doFilter(request, response);
      return;
    }
    var bucket = buckets.get(request.getRemoteAddr(), key -> newBucket());
    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
      return;
    }
    writeRateLimited(response);
  }

  private boolean isLoginRequest(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(normalize(request));
  }

  private static String normalize(HttpServletRequest request) {
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

  private static Bucket newBucket() {
    return Bucket.builder()
        .addLimit(
            limit ->
                limit.capacity(BUCKET_CAPACITY).refillIntervally(BUCKET_CAPACITY, REFILL_PERIOD))
        .build();
  }

  private void writeRateLimited(HttpServletResponse response) throws IOException {
    var body = new ApiError(RATE_LIMIT_ERROR_CODE, RATE_LIMIT_MESSAGE, clock.instant());
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
