package io.stablepay.api.domain.model.fixtures;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.Money;
import java.math.BigDecimal;

public final class MoneyFixtures {

  public static final Money SOME_MONEY = Money.fromMicros(100_000_000L, CurrencyCode.USD);

  public static final Money SOME_EUR_MONEY = Money.fromMicros(100_000_000L, CurrencyCode.EUR);

  public static final Money SOME_ZERO_MONEY = Money.fromMicros(0L, CurrencyCode.USD);

  private MoneyFixtures() {}

  public static Money someMoney(BigDecimal value, CurrencyCode currency) {
    return Money.builder().value(value).currency(currency).build();
  }
}
