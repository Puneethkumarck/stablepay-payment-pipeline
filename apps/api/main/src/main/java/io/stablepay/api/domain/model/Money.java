package io.stablepay.api.domain.model;

import com.neovisionaries.i18n.CurrencyCode;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record Money(BigDecimal value, CurrencyCode currency) {

  public Money {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(currency, "currency");
  }

  public static Money fromMicros(long micros, CurrencyCode currency) {
    return new Money(BigDecimal.valueOf(micros, 6), currency);
  }

  public long toMicros() {
    return value.movePointRight(6).longValueExact();
  }
}
