package io.stablepay.api.infrastructure.idempotency;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_ID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_CACHED_RESPONSE;
import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_IDEMPOTENCY_KEY;
import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.api.application.web.error.IdempotencyKeyRequiredException;
import io.stablepay.api.domain.port.IdempotencyRepository;
import io.stablepay.api.infrastructure.security.AuthenticatedUserToken;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(SOME_NOW, ZoneOffset.UTC);

  @Mock private IdempotencyRepository idempotencyRepository;
  @Mock private ProceedingJoinPoint pjp;
  @Mock private HttpServletRequest request;

  private IdempotencyAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new IdempotencyAspect(idempotencyRepository, new ObjectMapper(), FIXED_CLOCK);
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

  @Test
  void shouldThrowIdempotencyKeyRequiredWhenHeaderMissing() {
    // given
    given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY)).willReturn(null);

    // when / then
    assertThatThrownBy(() -> aspect.around(pjp))
        .isInstanceOf(IdempotencyKeyRequiredException.class);
  }

  @Test
  void shouldThrowIdempotencyKeyRequiredWhenHeaderBlank() {
    // given
    given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY)).willReturn("   ");

    // when / then
    assertThatThrownBy(() -> aspect.around(pjp))
        .isInstanceOf(IdempotencyKeyRequiredException.class);
  }

  @Test
  void shouldReplayCachedResponseOnCacheHit() throws Throwable {
    // given
    given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
        .willReturn(SOME_IDEMPOTENCY_KEY);
    given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
        .willReturn(Optional.of(SOME_CACHED_RESPONSE));

    // when
    var result = aspect.around(pjp);

    // then
    assertThat(result).isInstanceOf(ResponseEntity.class);
    var response = (ResponseEntity<?>) result;
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst(IdempotencyAspect.HEADER_IDEMPOTENCY_REPLAYED))
        .isEqualTo("true");
    assertThat(response.getBody()).isEqualTo(SOME_CACHED_RESPONSE.body());
    then(pjp).shouldHaveNoInteractions();
  }

  @Test
  void shouldProceedAndCacheOn2xxResponseWhenCacheMiss() throws Throwable {
    // given
    given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
        .willReturn(SOME_IDEMPOTENCY_KEY);
    given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
        .willReturn(Optional.empty());
    var responseBody = new TestDto("ok");
    given(pjp.proceed()).willReturn(responseBody);

    // when
    var result = aspect.around(pjp);

    // then
    assertThat(result).isEqualTo(responseBody);
    var captor = ArgumentCaptor.forClass(io.stablepay.api.domain.model.CachedResponse.class);
    then(idempotencyRepository)
        .should()
        .save(
            org.mockito.ArgumentMatchers.eq(SOME_IDEMPOTENCY_KEY),
            org.mockito.ArgumentMatchers.eq(SOME_ADMIN_USER_ID),
            captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(200);
    assertThat(captor.getValue().expiresAt())
        .isEqualTo(SOME_NOW.plus(IdempotencyAspect.DEFAULT_TTL));
  }

  @Test
  void shouldProceedWithoutCachingOnNon2xxResponse() throws Throwable {
    // given
    given(request.getHeader(IdempotencyAspect.HEADER_IDEMPOTENCY_KEY))
        .willReturn(SOME_IDEMPOTENCY_KEY);
    given(idempotencyRepository.findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW))
        .willReturn(Optional.empty());
    var errorResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found");
    given(pjp.proceed()).willReturn(errorResponse);

    // when
    var result = aspect.around(pjp);

    // then
    assertThat(result).isEqualTo(errorResponse);
    then(idempotencyRepository)
        .should()
        .findActive(SOME_IDEMPOTENCY_KEY, SOME_ADMIN_USER_ID, SOME_NOW);
    then(idempotencyRepository).shouldHaveNoMoreInteractions();
  }

  record TestDto(String value) {}
}
