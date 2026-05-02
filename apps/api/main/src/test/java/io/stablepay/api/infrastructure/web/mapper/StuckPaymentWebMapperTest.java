package io.stablepay.api.infrastructure.web.mapper;

import static io.stablepay.api.domain.model.fixtures.StuckPaymentFixtures.SOME_STUCK_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.infrastructure.web.dto.AmountDto;
import io.stablepay.api.infrastructure.web.dto.PaginatedResponse;
import io.stablepay.api.infrastructure.web.dto.StuckPaymentDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StuckPaymentWebMapperTest {

  private final StuckPaymentWebMapper mapper =
      new StuckPaymentWebMapperImpl(new AmountMapperImpl());

  @Test
  void shouldMapStuckPaymentToDto() {
    // given
    var stuckPayment = SOME_STUCK_PAYMENT;
    var expectedAmount =
        AmountDto.builder()
            .amountMicros(stuckPayment.amount().toMicros())
            .currencyCode(stuckPayment.amount().currency().name())
            .build();
    var expected =
        StuckPaymentDto.builder()
            .id(stuckPayment.id().value().toString())
            .reference(stuckPayment.reference())
            .flowType(stuckPayment.flowType())
            .internalStatus(stuckPayment.internalStatus())
            .customerId(stuckPayment.customerId().value().toString())
            .amount(expectedAmount)
            .lastEventAt(stuckPayment.lastEventAt())
            .stuckMillis(stuckPayment.stuckMillis())
            .build();

    // when
    var actual = mapper.toDto(stuckPayment);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldMapPaginatedResultToResponse() {
    // given
    var paginatedResult =
        PaginatedResult.<io.stablepay.api.domain.model.StuckPayment>builder()
            .items(List.of(SOME_STUCK_PAYMENT))
            .nextCursor(Optional.empty())
            .build();
    var expectedAmount =
        AmountDto.builder()
            .amountMicros(SOME_STUCK_PAYMENT.amount().toMicros())
            .currencyCode(SOME_STUCK_PAYMENT.amount().currency().name())
            .build();
    var expectedItem =
        StuckPaymentDto.builder()
            .id(SOME_STUCK_PAYMENT.id().value().toString())
            .reference(SOME_STUCK_PAYMENT.reference())
            .flowType(SOME_STUCK_PAYMENT.flowType())
            .internalStatus(SOME_STUCK_PAYMENT.internalStatus())
            .customerId(SOME_STUCK_PAYMENT.customerId().value().toString())
            .amount(expectedAmount)
            .lastEventAt(SOME_STUCK_PAYMENT.lastEventAt())
            .stuckMillis(SOME_STUCK_PAYMENT.stuckMillis())
            .build();
    var expected =
        PaginatedResponse.<StuckPaymentDto>builder()
            .items(List.of(expectedItem))
            .nextCursor(Optional.empty())
            .hasMore(false)
            .build();

    // when
    var actual = mapper.toResponse(paginatedResult);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
