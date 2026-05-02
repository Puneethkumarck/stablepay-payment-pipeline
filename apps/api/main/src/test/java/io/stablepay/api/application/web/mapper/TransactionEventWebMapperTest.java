package io.stablepay.api.application.web.mapper;

import static io.stablepay.api.domain.model.fixtures.TransactionEventFixtures.SOME_TRANSACTION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.TransactionEventDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TransactionEventWebMapperTest {

  private final TransactionEventWebMapper mapper =
      Mappers.getMapper(TransactionEventWebMapper.class);

  @Test
  void shouldMapDomainEventToDto() {
    // given
    var event = SOME_TRANSACTION_EVENT;

    // when
    var result = mapper.toDto(event);

    // then
    var expected =
        TransactionEventDto.builder()
            .eventId(event.eventId())
            .customerId(event.customerId())
            .status(event.status())
            .eventTime(event.eventTime())
            .amountMicros(event.amountMicros())
            .currencyCode(event.currencyCode())
            .flowType(event.flowType())
            .sortKey(event.sortKey())
            .build();
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
