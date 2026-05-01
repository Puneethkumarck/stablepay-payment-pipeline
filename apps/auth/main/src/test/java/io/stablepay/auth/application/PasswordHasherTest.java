package io.stablepay.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  private final PasswordHasher hasher = new PasswordHasher();

  @Test
  void matchesReturnsTrueForCorrectPassword() {
    var hash = hasher.hash("demo1234");

    var actual = hasher.matches("demo1234", hash);

    assertThat(actual).isTrue();
  }

  @Test
  void matchesReturnsFalseForIncorrectPassword() {
    var hash = hasher.hash("demo1234");

    var actual = hasher.matches("wrong-password", hash);

    assertThat(actual).isFalse();
  }

  @Test
  void hashProducesBcryptFormat() {
    var actual = hasher.hash("demo1234");

    assertThat(actual).startsWith("$2a$12$");
  }

  @Test
  void dummyHashIsStableAcrossCalls() {
    var first = hasher.dummyHash();
    var second = hasher.dummyHash();

    assertThat(second).isSameAs(first);
  }

  @Test
  void dummyHashIsValidBcryptHashThatRejectsArbitraryInput() {
    var actual = hasher.matches("any-password", hasher.dummyHash());

    assertThat(actual).isFalse();
  }
}
