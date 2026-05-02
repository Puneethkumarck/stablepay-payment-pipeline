package io.stablepay.api.application.web.mapper;

import static io.stablepay.api.domain.model.fixtures.FlowFixtures.SOME_FLOW;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.AmountDto;
import io.stablepay.api.application.web.dto.FlowDto;
import org.junit.jupiter.api.Test;

class FlowWebMapperTest {

  private final FlowWebMapper mapper = new FlowWebMapperImpl(new AmountMapperImpl());

  @Test
  void shouldMapFlowToDto() {
    // given
    var flow = SOME_FLOW;
    var expectedAmount =
        AmountDto.builder()
            .amountMicros(flow.totalAmount().toMicros())
            .currencyCode(flow.totalAmount().currency().name())
            .build();
    var expected =
        FlowDto.builder()
            .id(flow.id().value().toString())
            .flowType(flow.flowType())
            .status(flow.status())
            .customerId(flow.customerId().value().toString())
            .totalAmount(expectedAmount)
            .legCount(flow.legCount())
            .createdAt(flow.createdAt())
            .updatedAt(flow.updatedAt())
            .completedAt(flow.completedAt())
            .build();

    // when
    var actual = mapper.toDto(flow);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
