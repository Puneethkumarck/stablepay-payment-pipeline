package io.stablepay.api.infrastructure.trino;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.domain.model.fixtures.MoneyFixtures;
import io.stablepay.api.domain.model.fixtures.StuckPaymentFixtures;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrinoStuckRepositoryTest {

  @Mock ResultSet rs;

  @Test
  void shouldBuildStuckPaymentWhenAllColumnsPresent() throws Exception {
    // given
    given(rs.getString("transaction_id"))
        .willReturn(StuckPaymentFixtures.SOME_STUCK_TRANSACTION_ID.value().toString());
    given(rs.getString("transaction_reference")).willReturn("TXN-REF-STUCK-0001");
    given(rs.getString("flow_type")).willReturn("FIAT_PAYOUT");
    given(rs.getString("internal_status")).willReturn("AWAITING_CONFIRMATION");
    given(rs.getString("customer_id"))
        .willReturn(StuckPaymentFixtures.SOME_STUCK_CUSTOMER_ID.value().toString());
    given(rs.getLong("amount_micros")).willReturn(MoneyFixtures.SOME_MONEY.toMicros());
    given(rs.getString("currency_code")).willReturn(MoneyFixtures.SOME_MONEY.currency().name());
    given(rs.getTimestamp("last_event_at"))
        .willReturn(Timestamp.from(StuckPaymentFixtures.SOME_STUCK_LAST_EVENT_AT));
    given(rs.getLong("stuck_millis")).willReturn(900_000L);
    var expected = StuckPaymentFixtures.SOME_STUCK_PAYMENT;

    // when
    var actual = TrinoStuckRepository.STUCK_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldRoundTripCursorEncodeAndDecode() {
    // given
    var transactionIdString = StuckPaymentFixtures.SOME_STUCK_TRANSACTION_ID.value().toString();
    var encoded = TrinoStuckRepository.encodeCursor(900_000L, transactionIdString);
    var expected = new TrinoStuckCursor(900_000L, transactionIdString);

    // when
    var actual = TrinoStuckRepository.decodeCursor(encoded);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
