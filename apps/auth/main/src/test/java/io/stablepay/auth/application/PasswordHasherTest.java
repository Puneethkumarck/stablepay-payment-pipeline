package io.stablepay.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  private final PasswordHasher hasher = new PasswordHasher();

  @Test
  void matchesReturnsTrueForCorrectPassword() {
    // given
    var hash = hasher.hash("demo1234");

    // when
    var actual = hasher.matches("demo1234", hash);

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void matchesReturnsFalseForIncorrectPassword() {
    // given
    var hash = hasher.hash("demo1234");

    // when
    var actual = hasher.matches("wrong-password", hash);

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void hashProducesBcryptFormat() {
    // when
    var actual = hasher.hash("demo1234");

    // then
    assertThat(actual).startsWith("$2a$12$");
  }

  @Test
  void dummyHashIsStableAcrossCalls() {
    // given
    var first = hasher.dummyHash();

    // when
    var second = hasher.dummyHash();

    // then
    assertThat(second).isSameAs(first);
  }

  @Test
  void dummyHashIsValidBcryptHashThatRejectsArbitraryInput() {
    // when
    var actual = hasher.matches("any-password", hasher.dummyHash());

    // then
    assertThat(actual).isFalse();
  }
}
