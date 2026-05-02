package io.stablepay.api.infrastructure.sse;

import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.domain.model.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TransactionEventMapperTest {

  private final TransactionEventMapper mapper = Mappers.getMapper(TransactionEventMapper.class);

  @Test
  void shouldMapTransactionToCompactEvent() {
    // given
    var tx = SOME_TRANSACTION;

    // when
    var result = mapper.toEvent(tx);

    // then
    var expected =
        TransactionEvent.builder()
            .eventId(tx.eventId())
            .customerId(tx.customerId().value().toString())
            .status(tx.customerStatus())
            .eventTime(tx.eventTime())
            .amountMicros(tx.amount().toMicros())
            .currencyCode(tx.amount().currency().name())
            .flowType(tx.flowType())
            .sortKey("")
            .build();
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
