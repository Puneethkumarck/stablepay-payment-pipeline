package io.stablepay.api.application.web.mapper;

import static io.stablepay.api.domain.model.fixtures.CustomerSummaryFixtures.SOME_CUSTOMER_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.AmountDto;
import io.stablepay.api.application.web.dto.CustomerSummaryDto;
import org.junit.jupiter.api.Test;

class CustomerSummaryWebMapperTest {

  private final CustomerSummaryWebMapper mapper =
      new CustomerSummaryWebMapperImpl(new AmountMapperImpl());

  @Test
  void shouldMapCustomerSummaryToDto() {
    // given
    var summary = SOME_CUSTOMER_SUMMARY;
    var expectedBalance =
        AmountDto.builder()
            .amountMicros(summary.balance().toMicros())
            .currencyCode(summary.balance().currency().name())
            .build();
    var expectedTotalSent =
        AmountDto.builder()
            .amountMicros(summary.totalSent().toMicros())
            .currencyCode(summary.totalSent().currency().name())
            .build();
    var expected =
        CustomerSummaryDto.builder()
            .id(summary.id().value().toString())
            .name(summary.name())
            .email(summary.email())
            .kyc(summary.kyc())
            .balance(expectedBalance)
            .totalSent(expectedTotalSent)
            .txnCount(summary.txnCount())
            .joined(summary.joined())
            .risk(summary.risk())
            .build();

    // when
    var actual = mapper.toDto(summary);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
