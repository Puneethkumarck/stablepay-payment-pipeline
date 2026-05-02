package io.stablepay.api.domain.model.fixtures;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.Money;
import java.math.BigDecimal;

public final class MoneyFixtures {

  public static final Money SOME_MONEY =
      Money.builder().value(new BigDecimal("100.00")).currency(CurrencyCode.USD).build();

  public static final Money SOME_EUR_MONEY =
      Money.builder().value(new BigDecimal("100.00")).currency(CurrencyCode.EUR).build();

  public static final Money SOME_ZERO_MONEY =
      Money.builder().value(new BigDecimal("0.00")).currency(CurrencyCode.USD).build();

  private MoneyFixtures() {}

  public static Money someMoney(BigDecimal value, CurrencyCode currency) {
    return Money.builder().value(value).currency(currency).build();
  }
}
