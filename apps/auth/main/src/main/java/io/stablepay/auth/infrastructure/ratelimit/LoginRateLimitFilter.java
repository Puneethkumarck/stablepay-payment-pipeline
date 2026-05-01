package io.stablepay.auth.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.stablepay.auth.client.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
  private static final String RATE_LIMIT_MESSAGE =
      "Too many login attempts. Try again in 60 seconds.";
  private static final long RETRY_AFTER_SECONDS = 60L;
  private static final int BUCKET_CAPACITY = 5;
  private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!isLoginRequest(request)) {
      chain.doFilter(request, response);
      return;
    }
    var bucket = buckets.computeIfAbsent(clientIp(request), key -> newBucket());
    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
      return;
    }
    writeRateLimited(response);
  }

  private boolean isLoginRequest(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod())
        && LOGIN_PATH.equals(request.getRequestURI());
  }

  private String clientIp(HttpServletRequest request) {
    var forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private Bucket newBucket() {
    var bandwidth =
        Bandwidth.builder()
            .capacity(BUCKET_CAPACITY)
            .refillIntervally(BUCKET_CAPACITY, REFILL_PERIOD)
            .build();
    return Bucket.builder().addLimit(bandwidth).build();
  }

  private void writeRateLimited(HttpServletResponse response) throws IOException {
    var body = new ApiError(RATE_LIMIT_ERROR_CODE, RATE_LIMIT_MESSAGE, clock.instant());
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
