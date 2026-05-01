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
  void buildsActiveSigningKey() {
    var actual = activeSigningKey();

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
  void toBuilderDeactivatesKey() {
    var actual = activeSigningKey().toBuilder().isActive(false).build();

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
}
