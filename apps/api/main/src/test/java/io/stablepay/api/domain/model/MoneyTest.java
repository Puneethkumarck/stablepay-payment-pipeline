package io.stablepay.api.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.fixtures.MoneyFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void fromMicros_andToMicros_roundTrip() {
    // given
    var micros = 150_000_000L;

    // when
    var actual = Money.fromMicros(micros, CurrencyCode.USD).toMicros();

    // then
    assertThat(actual).isEqualTo(micros);
  }

  @Test
  void fromMicros_zero() {
    // given
    var micros = 0L;

    // when
    var actual = Money.fromMicros(micros, CurrencyCode.USD).toMicros();

    // then
    assertThat(actual).isEqualTo(0L);
  }

  @Test
  void fromMicros_negative() {
    // given
    var micros = -1_000_000L;

    // when
    var actual = Money.fromMicros(micros, CurrencyCode.USD).toMicros();

    // then
    assertThat(actual).isEqualTo(-1_000_000L);
  }

  @Test
  void builder_buildsExpectedMoney_recursiveComparison() {
    // given
    var expected = new Money(BigDecimal.valueOf(100.50), CurrencyCode.EUR);

    // when
    var actual =
        Money.builder().value(BigDecimal.valueOf(100.50)).currency(CurrencyCode.EUR).build();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void requireNonNull_value_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> new Money(null, CurrencyCode.USD))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  @Test
  void requireNonNull_currency_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("currency");
  }

  @Test
  void toBuilder_changesCurrency_recursiveComparison() {
    // given
    var expected = new Money(MoneyFixtures.SOME_MONEY.value(), CurrencyCode.EUR);

    // when
    var actual = MoneyFixtures.SOME_MONEY.toBuilder().currency(CurrencyCode.EUR).build();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void toMicros_precisionLoss_throwsArithmeticException() {
    // given
    var money = new Money(new BigDecimal("100.1234567"), CurrencyCode.USD);

    // when / then
    assertThatThrownBy(money::toMicros).isInstanceOf(ArithmeticException.class);
  }

  @Test
  void toMicros_overflow_throwsArithmeticException() {
    // given
    var money = new Money(new BigDecimal("1e20"), CurrencyCode.USD);

    // when / then
    assertThatThrownBy(money::toMicros).isInstanceOf(ArithmeticException.class);
  }
}
