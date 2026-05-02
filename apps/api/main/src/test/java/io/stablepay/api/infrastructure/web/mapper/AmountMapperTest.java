package io.stablepay.api.infrastructure.web.mapper;

import static io.stablepay.api.domain.model.fixtures.MoneyFixtures.SOME_MONEY;
import static org.assertj.core.api.Assertions.assertThat;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.infrastructure.web.dto.AmountDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AmountMapperTest {

  private final AmountMapper mapper = Mappers.getMapper(AmountMapper.class);

  @Test
  void shouldMapMoneyToAmountDto() {
    // given
    var money = SOME_MONEY;
    var expected = AmountDto.builder().amountMicros(100_000_000L).currencyCode("USD").build();

    // when
    var actual = mapper.toDto(money);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldMapAmountDtoToMoney() {
    // given
    var dto = AmountDto.builder().amountMicros(100_000_000L).currencyCode("USD").build();
    var expected = Money.fromMicros(100_000_000L, CurrencyCode.USD);

    // when
    var actual = mapper.toDomain(dto);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
