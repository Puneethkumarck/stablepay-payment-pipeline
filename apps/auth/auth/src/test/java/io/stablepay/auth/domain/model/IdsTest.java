package io.stablepay.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdsTest {

  private static final UUID SOME_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void shouldWrapUuidInUserId() {
    // when
    var actual = UserId.of(SOME_UUID);

    // then
    var expected = new UserId(SOME_UUID);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldWrapUuidInCustomerId() {
    // when
    var actual = CustomerId.of(SOME_UUID);

    // then
    var expected = new CustomerId(SOME_UUID);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldWrapUuidInRefreshTokenId() {
    // when
    var actual = RefreshTokenId.of(SOME_UUID);

    // then
    var expected = new RefreshTokenId(SOME_UUID);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
