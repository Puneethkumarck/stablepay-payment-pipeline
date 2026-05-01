package io.stablepay.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdsTest {

  private static final UUID SOME_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void userIdOfWrapsUuid() {
    var actual = UserId.of(SOME_UUID);

    var expected = new UserId(SOME_UUID);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void customerIdOfWrapsUuid() {
    var actual = CustomerId.of(SOME_UUID);

    var expected = new CustomerId(SOME_UUID);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void refreshTokenIdOfWrapsUuid() {
    var actual = RefreshTokenId.of(SOME_UUID);

    var expected = new RefreshTokenId(SOME_UUID);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
