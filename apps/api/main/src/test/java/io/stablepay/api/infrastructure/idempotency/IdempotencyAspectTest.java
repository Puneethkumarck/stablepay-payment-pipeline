package io.stablepay.api.infrastructure.idempotency;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_ID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_CACHED_RESPONSE;
import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_EXPIRES_AT;
import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_IDEMPOTENCY_KEY;
import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_NOW;
import static io.stablepay.api.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.api.application.web.error.IdempotencyKeyConflictException;
import io.stablepay.api.application.web.error.IdempotencyKeyRequiredException;
import io.stablepay.api.domain.model.CachedResponse;
import io.stablepay.api.domain.port.IdempotencyRepository;
import io.stablepay.api.infrastructure.security.AuthenticatedUserToken;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(SOME_NOW, ZoneOffset.UTC);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private IdempotencyRepository idempotencyRepository;
  @Mock private ProceedingJoinPoint pjp;
  @Mock private HttpServletRequest request;

  private IdempotencyAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new IdempotencyAspect(idempotencyRepository, OBJECT_MAPPER, FIXED_CLOCK);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    var user = someAdminUser();
    var jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .claim("sub", user.userId().value().toString())
            .build();
    var token =
        new AuthenticatedUserToken(jwt, user, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
  }

  @Nested
  class WhenHeaderMissing {

    @Test
    void shouldThrowWhenHeaderNull() {
      // given
      given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY)).willReturn(null);

      // when / then
      assertThatThrownBy(() -> aspect.around(pjp))
          .isInstanceOf(IdempotencyKeyRequiredException.class)
          .hasMessage("X-Idempotency-Key header is required");
    }

    @Test
    void shouldThrowWhenHeaderBlank() {
      // given
      given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY)).willReturn("   ");

      // when / then
      assertThatThrownBy(() -> aspect.around(pjp))
          .isInstanceOf(IdempotencyKeyRequiredException.class);
    }
  }

  @Nested
  class WhenCacheHit {

    @Test
    void shouldReplayCachedResponse() throws Throwable {
      // given
      given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
          .willReturn(SOME_IDEMPOTENCY_KEY);
      given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
          .willReturn(Optional.of(SOME_CACHED_RESPONSE));

      // when
      var result = aspect.around(pjp);

      // then
      var expectedHeaders = new HttpHeaders();
      expectedHeaders.set(IdempotencyAspect.HEADER_IDEMPOTENCY_REPLAYED, "true");
      expectedHeaders.setContentType(MediaType.APPLICATION_JSON);
      var expected =
          new ResponseEntity<>(SOME_CACHED_RESPONSE.body(), expectedHeaders, HttpStatus.OK);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
      then(pjp).shouldHaveNoInteractions();
    }
  }

  @Nested
  class WhenCacheMiss {

    @Test
    void shouldProceedAndCacheOn2xxResponse() throws Throwable {
      // given
      given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
          .willReturn(SOME_IDEMPOTENCY_KEY);
      given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
          .willReturn(Optional.empty());
      given(
              idempotencyRepository.tryAcquire(
                  SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_EXPIRES_AT))
          .willReturn(true);
      var responseBody = Map.of("value", "ok");
      given(pjp.proceed()).willReturn(responseBody);

      // when
      var result = aspect.around(pjp);

      // then
      assertThat(result).isEqualTo(responseBody);
      var expectedCachedResponse =
          CachedResponse.builder()
              .status(200)
              .body(OBJECT_MAPPER.writeValueAsBytes(responseBody))
              .expiresAt(SOME_EXPIRES_AT)
              .build();
      then(idempotencyRepository)
          .should()
          .save(
              eqIgnoring(SOME_IDEMPOTENCY_KEY),
              eqIgnoring(SOME_ADMIN_USER_ID),
              eqIgnoring(expectedCachedResponse));
    }

    @Test
    void shouldProceedWithoutCachingOnNon2xxResponse() throws Throwable {
      // given
      given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
          .willReturn(SOME_IDEMPOTENCY_KEY);
      given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
          .willReturn(Optional.empty());
      given(
              idempotencyRepository.tryAcquire(
                  SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_EXPIRES_AT))
          .willReturn(true);
      var errorResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found");
      given(pjp.proceed()).willReturn(errorResponse);

      // when
      var result = aspect.around(pjp);

      // then
      assertThat(result).isEqualTo(errorResponse);
      then(idempotencyRepository)
          .should()
          .findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW);
      then(idempotencyRepository)
          .should()
          .tryAcquire(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_EXPIRES_AT);
      then(idempotencyRepository).shouldHaveNoMoreInteractions();
    }
  }

  @Nested
  class WhenConcurrentRequest {

    @Test
    void shouldReplayWhenAcquireFailsAndResponseAvailable() throws Throwable {
      // given
      given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
          .willReturn(SOME_IDEMPOTENCY_KEY);
      given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
          .willReturn(Optional.empty())
          .willReturn(Optional.of(SOME_CACHED_RESPONSE));
      given(
              idempotencyRepository.tryAcquire(
                  SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_EXPIRES_AT))
          .willReturn(false);

      // when
      var result = aspect.around(pjp);

      // then
      var expectedHeaders = new HttpHeaders();
      expectedHeaders.set(IdempotencyAspect.HEADER_IDEMPOTENCY_REPLAYED, "true");
      expectedHeaders.setContentType(MediaType.APPLICATION_JSON);
      var expected =
          new ResponseEntity<>(SOME_CACHED_RESPONSE.body(), expectedHeaders, HttpStatus.OK);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
      then(pjp).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowConflictWhenAcquireFailsAndResponseNotAvailable() {
      // given
      given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
          .willReturn(SOME_IDEMPOTENCY_KEY);
      given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
          .willReturn(Optional.empty());
      given(
              idempotencyRepository.tryAcquire(
                  SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_EXPIRES_AT))
          .willReturn(false);

      // when / then
      assertThatThrownBy(() -> aspect.around(pjp))
          .isInstanceOf(IdempotencyKeyConflictException.class);
      then(pjp).shouldHaveNoInteractions();
    }
  }

}
