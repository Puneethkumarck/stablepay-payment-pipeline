package io.stablepay.api.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.error.IdempotencyKeyConflictException;
import io.stablepay.api.application.web.error.IdempotencyKeyRequiredException;
import io.stablepay.api.domain.model.CachedResponse;
import io.stablepay.api.domain.port.IdempotencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IdempotencyAspect {

  static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";
  static final String HEADER_IDEMPOTENCY_REPLAYED = "Idempotency-Replayed";
  static final Duration DEFAULT_TTL = Duration.ofHours(24);

  private final IdempotencyRepository idempotencyRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Around("@annotation(io.stablepay.api.application.web.annotation.Idempotent)")
  public Object around(ProceedingJoinPoint pjp) throws Throwable {
    var request = currentRequest();
    var idempotencyKey = extractIdempotencyKey(request);
    var user = extractAuthenticatedUser();
    var now = clock.instant();

    var cached = idempotencyRepository.findActive(idempotencyKey, user.userId(), now);
    if (cached.isPresent()) {
      log.info("Idempotency cache hit for key={}", idempotencyKey);
      return replayResponse(cached.get());
    }

    var expiresAt = now.plus(DEFAULT_TTL);
    if (!idempotencyRepository.tryAcquire(idempotencyKey, user.userId(), expiresAt)) {
      var replay = idempotencyRepository.findActive(idempotencyKey, user.userId(), clock.instant());
      if (replay.isPresent()) {
        log.info("Idempotency cache hit on retry for key={}", idempotencyKey);
        return replayResponse(replay.get());
      }
      throw new IdempotencyKeyConflictException(idempotencyKey);
    }

    var result = pjp.proceed();
    cacheIfSuccessful(idempotencyKey, user, result);
    return result;
  }

  private String extractIdempotencyKey(HttpServletRequest request) {
    var key = request.getHeader(HEADER_IDEMPOTENCY_KEY);
    if (key == null || key.isBlank()) {
      throw new IdempotencyKeyRequiredException();
    }
    return key.strip();
  }

  private AuthenticatedUser extractAuthenticatedUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    Objects.requireNonNull(authentication, "authentication");
    return (AuthenticatedUser) authentication.getPrincipal();
  }

  private ResponseEntity<byte[]> replayResponse(CachedResponse cached) {
    var headers = new HttpHeaders();
    headers.set(HEADER_IDEMPOTENCY_REPLAYED, "true");
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>(cached.body(), headers, HttpStatus.valueOf(cached.status()));
  }

  private void cacheIfSuccessful(String key, AuthenticatedUser user, Object result) {
    try {
      var status = resolveStatus(result);
      if (status >= 200 && status < 300) {
        var body = serializeBody(result);
        var expiresAt = clock.instant().plus(DEFAULT_TTL);
        var response =
            CachedResponse.builder().status(status).body(body).expiresAt(expiresAt).build();
        idempotencyRepository.save(key, user.userId(), response);
      }
    } catch (Exception e) {
      log.error("Failed to cache idempotent response for key={}: {}", key, e.getMessage());
    }
  }

  private int resolveStatus(Object result) {
    if (result instanceof ResponseEntity<?> re) {
      return re.getStatusCode().value();
    }
    return 200;
  }

  private byte[] serializeBody(Object result) throws Exception {
    if (result instanceof ResponseEntity<?> re) {
      return objectMapper.writeValueAsBytes(re.getBody());
    }
    return objectMapper.writeValueAsBytes(result);
  }

  private HttpServletRequest currentRequest() {
    var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    return attrs.getRequest();
  }
}
