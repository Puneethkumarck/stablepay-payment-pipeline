package io.stablepay.flink.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PiiMaskerTest {

  @Nested
  class WhenInputIsNullOrEmpty {

    @Test
    void shouldReturnNullForNullInput() {
      // when
      var result = PiiMasker.mask(null);

      // then
      assertThat(result).isNull();
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
      // when
      var result = PiiMasker.mask("");

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class WhenInputIsShorterThanPrefix {

    @Test
    void shouldFullyMaskSingleCharacter() {
      // when
      var result = PiiMasker.mask("A");

      // then
      assertThat(result).isEqualTo("*");
    }

    @Test
    void shouldFullyMaskStringAtPrefixLength() {
      // when
      var result = PiiMasker.mask("abcd");

      // then
      assertThat(result).isEqualTo("****");
    }
  }

  @Nested
  class WhenInputIsLongerThanPrefix {

    @Test
    void shouldPreservePrefixAndMaskRemainder() {
      // when
      var result = PiiMasker.mask("John Doe");

      // then
      assertThat(result).isEqualTo("John****");
    }

    @Test
    void shouldMaskUuidPreservingFirstFourChars() {
      // when
      var result = PiiMasker.mask("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

      // then
      assertThat(result).startsWith("a1b2");
      assertThat(result).hasSize(36);
      assertThat(result.substring(4)).matches("\\*+");
    }
  }
}
