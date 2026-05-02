package io.stablepay.api.application.web.mapper;

import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.AmountDto;
import io.stablepay.api.application.web.dto.PaginatedResponse;
import io.stablepay.api.application.web.dto.TransactionDto;
import io.stablepay.api.domain.model.PaginatedResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransactionWebMapperTest {

  private final TransactionWebMapper mapper = new TransactionWebMapperImpl(new AmountMapperImpl());

  @Test
  void shouldMapTransactionToDto() {
    // given
    var transaction = SOME_TRANSACTION;
    var expectedAmount =
        AmountDto.builder()
            .amountMicros(transaction.amount().toMicros())
            .currencyCode(transaction.amount().currency().name())
            .build();
    var expected =
        TransactionDto.builder()
            .id(transaction.id().value().toString())
            .reference(transaction.reference())
            .flowType(transaction.flowType())
            .internalStatus(transaction.internalStatus())
            .customerStatus(transaction.customerStatus())
            .amount(expectedAmount)
            .customerId(transaction.customerId().value().toString())
            .accountId(transaction.accountId().value().toString())
            .counterparty(transaction.counterparty())
            .flowId(transaction.flowId().value().toString())
            .eventTime(transaction.eventTime())
            .ingestTime(transaction.ingestTime())
            .build();

    // when
    var actual = mapper.toDto(transaction);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldMapPaginatedResultToResponse() {
    // given
    var paginatedResult =
        PaginatedResult.<io.stablepay.api.domain.model.Transaction>builder()
            .items(List.of(SOME_TRANSACTION))
            .nextCursor(Optional.of("cursor-abc"))
            .build();
    var expectedAmount =
        AmountDto.builder()
            .amountMicros(SOME_TRANSACTION.amount().toMicros())
            .currencyCode(SOME_TRANSACTION.amount().currency().name())
            .build();
    var expectedItem =
        TransactionDto.builder()
            .id(SOME_TRANSACTION.id().value().toString())
            .reference(SOME_TRANSACTION.reference())
            .flowType(SOME_TRANSACTION.flowType())
            .internalStatus(SOME_TRANSACTION.internalStatus())
            .customerStatus(SOME_TRANSACTION.customerStatus())
            .amount(expectedAmount)
            .customerId(SOME_TRANSACTION.customerId().value().toString())
            .accountId(SOME_TRANSACTION.accountId().value().toString())
            .counterparty(SOME_TRANSACTION.counterparty())
            .flowId(SOME_TRANSACTION.flowId().value().toString())
            .eventTime(SOME_TRANSACTION.eventTime())
            .ingestTime(SOME_TRANSACTION.ingestTime())
            .build();
    var expected =
        PaginatedResponse.<TransactionDto>builder()
            .items(List.of(expectedItem))
            .nextCursor(Optional.of("cursor-abc"))
            .hasMore(true)
            .build();

    // when
    var actual = mapper.toResponse(paginatedResult);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
