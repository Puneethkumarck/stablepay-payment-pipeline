package io.stablepay.auth.domain.model;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_EXPIRES_AT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_LATER_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_REFRESH_TOKEN_ID;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_TOKEN_HASH;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_USER_ID;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.activeRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  void buildsActiveTokenWithEmptyRevokedAt() {
    var actual = activeRefreshToken();

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
  void isActiveReturnsTrueWhenNotRevokedAndNotExpired() {
    var token = activeRefreshToken();

    var actual = token.isActive(SOME_LATER_INSTANT);

    assertThat(actual).isTrue();
  }

  @Test
  void isActiveReturnsFalseWhenExpired() {
    var token = activeRefreshToken();
    var afterExpiry = SOME_EXPIRES_AT.plusSeconds(1);

    var actual = token.isActive(afterExpiry);

    assertThat(actual).isFalse();
  }

  @Test
  void isActiveReturnsFalseAtExactExpiryInstant() {
    var token = activeRefreshToken();

    var actual = token.isActive(SOME_EXPIRES_AT);

    assertThat(actual).isFalse();
  }

  @Test
  void isActiveReturnsFalseWhenRevokedEvenIfNotExpired() {
    var token = activeRefreshToken().revoke(SOME_LATER_INSTANT);

    var actual = token.isActive(SOME_LATER_INSTANT);

    assertThat(actual).isFalse();
  }

  @Test
  void revokeReturnsNewInstanceWithRevokedAtSet() {
    var actual = activeRefreshToken().revoke(SOME_LATER_INSTANT);

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
  void revokeDoesNotMutateOriginalInstance() {
    var original = activeRefreshToken();

    original.revoke(SOME_LATER_INSTANT);

    assertThat(original.revokedAt()).isEqualTo(Optional.<Instant>empty());
  }
}
