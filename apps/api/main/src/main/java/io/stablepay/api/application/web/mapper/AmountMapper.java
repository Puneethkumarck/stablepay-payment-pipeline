package io.stablepay.api.application.web.mapper;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.application.web.dto.AmountDto;
import io.stablepay.api.domain.model.Money;
import org.mapstruct.Mapper;

@Mapper
public interface AmountMapper {

  default AmountDto toDto(Money money) {
    return AmountDto.builder()
        .amountMicros(money.toMicros())
        .currencyCode(money.currency().name())
        .build();
  }

  default Money toDomain(AmountDto dto) {
    return Money.fromMicros(dto.amountMicros(), CurrencyCode.valueOf(dto.currencyCode()));
  }
}
