package io.stablepay.api.infrastructure.trino;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.domain.model.Flow;
import io.stablepay.api.domain.model.fixtures.FlowFixtures;
import io.stablepay.api.domain.model.fixtures.MoneyFixtures;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrinoFlowRepositoryTest {

  @Mock ResultSet rs;

  @Test
  void shouldBuildFlowWhenAllColumnsPresent() throws Exception {
    // given
    given(rs.getString("flow_id")).willReturn(FlowFixtures.SOME_FLOW_ID.value().toString());
    given(rs.getString("flow_type")).willReturn("MULTI_LEG");
    given(rs.getString("status")).willReturn("IN_PROGRESS");
    given(rs.getString("customer_id"))
        .willReturn(FlowFixtures.SOME_FLOW_CUSTOMER_ID.value().toString());
    given(rs.getLong("total_amount_micros")).willReturn(MoneyFixtures.SOME_MONEY.toMicros());
    given(rs.getString("total_currency_code"))
        .willReturn(MoneyFixtures.SOME_MONEY.currency().name());
    given(rs.getInt("leg_count")).willReturn(3);
    given(rs.getTimestamp("created_at"))
        .willReturn(Timestamp.from(FlowFixtures.SOME_FLOW_CREATED_AT));
    given(rs.getTimestamp("updated_at"))
        .willReturn(Timestamp.from(FlowFixtures.SOME_FLOW_UPDATED_AT));
    given(rs.getTimestamp("completed_at")).willReturn(null);
    var expected = FlowFixtures.SOME_FLOW;

    // when
    var actual = TrinoFlowRepository.FLOW_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldBuildFlowWithCompletedAtWhenCompletedAtPresent() throws Exception {
    // given
    var completedAt = FlowFixtures.SOME_FLOW_UPDATED_AT;
    given(rs.getString("flow_id")).willReturn(FlowFixtures.SOME_FLOW_ID.value().toString());
    given(rs.getString("flow_type")).willReturn("MULTI_LEG");
    given(rs.getString("status")).willReturn("COMPLETED");
    given(rs.getString("customer_id"))
        .willReturn(FlowFixtures.SOME_FLOW_CUSTOMER_ID.value().toString());
    given(rs.getLong("total_amount_micros")).willReturn(MoneyFixtures.SOME_MONEY.toMicros());
    given(rs.getString("total_currency_code"))
        .willReturn(MoneyFixtures.SOME_MONEY.currency().name());
    given(rs.getInt("leg_count")).willReturn(3);
    given(rs.getTimestamp("created_at"))
        .willReturn(Timestamp.from(FlowFixtures.SOME_FLOW_CREATED_AT));
    given(rs.getTimestamp("updated_at"))
        .willReturn(Timestamp.from(FlowFixtures.SOME_FLOW_UPDATED_AT));
    given(rs.getTimestamp("completed_at")).willReturn(Timestamp.from(completedAt));
    var expected =
        Flow.builder()
            .id(FlowFixtures.SOME_FLOW_ID)
            .flowType("MULTI_LEG")
            .status("COMPLETED")
            .customerId(FlowFixtures.SOME_FLOW_CUSTOMER_ID)
            .totalAmount(MoneyFixtures.SOME_MONEY)
            .legCount(3)
            .createdAt(FlowFixtures.SOME_FLOW_CREATED_AT)
            .updatedAt(FlowFixtures.SOME_FLOW_UPDATED_AT)
            .completedAt(Optional.of(completedAt))
            .build();

    // when
    var actual = TrinoFlowRepository.FLOW_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldRoundTripCursorEncodeAndDecode() {
    // given
    var createdAtMillis = FlowFixtures.SOME_FLOW_CREATED_AT.toEpochMilli();
    var flowIdString = FlowFixtures.SOME_FLOW_ID.value().toString();
    var encoded = TrinoFlowRepository.encodeCursor(createdAtMillis, flowIdString);
    var expected =
        new TrinoFlowRepository.DecodedCursor(FlowFixtures.SOME_FLOW_CREATED_AT, flowIdString);

    // when
    var actual = TrinoFlowRepository.decodeCursor(encoded);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
