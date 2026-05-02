package io.stablepay.auth.domain.model;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_EXPIRES_AT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_LATER_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_REFRESH_TOKEN_ID;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_TOKEN_HASH;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_USER_ID;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.activeRefreshToken;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.refreshTokenBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  void shouldBuildActiveTokenWithEmptyRevokedAt() {
    // when
    var actual = refreshTokenBuilder().id(SOME_REFRESH_TOKEN_ID).build();

    // then
    var expected =
        new RefreshToken(
            SOME_REFRESH_TOKEN_ID,
            SOME_USER_ID,
            SOME_TOKEN_HASH,
            SOME_INSTANT,
            SOME_EXPIRES_AT,
            Optional.empty());
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturnActiveWhenNotRevokedAndNotExpired() {
    // given
    var token = activeRefreshToken();

    // when
    var actual = token.isActive(SOME_LATER_INSTANT);

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldReturnInactiveWhenExpired() {
    // given
    var token = activeRefreshToken();
    var afterExpiry = SOME_EXPIRES_AT.plusSeconds(1);

    // when
    var actual = token.isActive(afterExpiry);

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void shouldReturnInactiveAtExactExpiryInstant() {
    // given
    var token = activeRefreshToken();

    // when
    var actual = token.isActive(SOME_EXPIRES_AT);

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void shouldReturnInactiveWhenRevokedEvenIfNotExpired() {
    // given
    var token = activeRefreshToken().revoke(SOME_LATER_INSTANT);

    // when
    var actual = token.isActive(SOME_LATER_INSTANT);

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void shouldReturnNewInstanceWithRevokedAtSetWhenRevoked() {
    // when
    var actual = refreshTokenBuilder().id(SOME_REFRESH_TOKEN_ID).build().revoke(SOME_LATER_INSTANT);

    // then
    var expected =
        new RefreshToken(
            SOME_REFRESH_TOKEN_ID,
            SOME_USER_ID,
            SOME_TOKEN_HASH,
            SOME_INSTANT,
            SOME_EXPIRES_AT,
            Optional.of(SOME_LATER_INSTANT));
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldNotMutateOriginalInstanceWhenRevoked() {
    // given
    var original = activeRefreshToken();

    // when
    original.revoke(SOME_LATER_INSTANT);

    // then
    assertThat(original.revokedAt()).isEqualTo(Optional.<Instant>empty());
  }
}
