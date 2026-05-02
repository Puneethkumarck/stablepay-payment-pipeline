package io.stablepay.api.application.web.mapper;

import static io.stablepay.api.domain.model.fixtures.DashboardStatsFixtures.SOME_DASHBOARD_STATS;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.DashboardStatsDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DashboardStatsWebMapperTest {

  private final DashboardStatsWebMapper mapper = Mappers.getMapper(DashboardStatsWebMapper.class);

  @Test
  void shouldMapDashboardStatsToDto() {
    // given
    var stats = SOME_DASHBOARD_STATS;
    var expected =
        DashboardStatsDto.builder()
            .volume24hMicros(stats.volume24hMicros())
            .currencyCode(stats.currencyCode().name())
            .transactionCount(stats.transactionCount())
            .successRate(stats.successRate())
            .dlqCount(stats.dlqCount())
            .dlqCriticalCount(stats.dlqCriticalCount())
            .stuckCount(stats.stuckCount())
            .stuckCriticalCount(stats.stuckCriticalCount())
            .periodStart(stats.periodStart())
            .periodEnd(stats.periodEnd())
            .build();

    // when
    var actual = mapper.toDto(stats);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
