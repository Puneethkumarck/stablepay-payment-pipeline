package io.stablepay.auth.domain.model;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_ALGORITHM;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_KID;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_PRIVATE_KEY_PEM;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_PUBLIC_KEY_PEM;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.activeSigningKey;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SigningKeyTest {

  @Test
  void shouldBuildActiveSigningKey() {
    // when
    var actual = activeSigningKey();

    // then
    var expected =
        new SigningKey(
            SOME_KID,
            SOME_PRIVATE_KEY_PEM,
            SOME_PUBLIC_KEY_PEM,
            SOME_ALGORITHM,
            SOME_INSTANT,
            true);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldDeactivateSigningKeyViaToBuilder() {
    // when
    var actual = activeSigningKey().toBuilder().isActive(false).build();

    // then
    var expected =
        new SigningKey(
            SOME_KID,
            SOME_PRIVATE_KEY_PEM,
            SOME_PUBLIC_KEY_PEM,
            SOME_ALGORITHM,
            SOME_INSTANT,
            false);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldRedactPrivateKeyPemInToString() {
    // given
    var key = activeSigningKey();

    // when
    var actual = key.toString();

    // then
    assertThat(actual).contains("***REDACTED***").doesNotContain(SOME_PRIVATE_KEY_PEM);
  }
}
