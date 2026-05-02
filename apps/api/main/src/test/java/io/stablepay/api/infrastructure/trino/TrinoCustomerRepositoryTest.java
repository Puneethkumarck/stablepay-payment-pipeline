package io.stablepay.api.infrastructure.trino;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.domain.model.fixtures.CustomerSummaryFixtures;
import io.stablepay.api.domain.model.fixtures.MoneyFixtures;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrinoCustomerRepositoryTest {

  @Mock ResultSet rs;

  @Test
  void shouldBuildCustomerSummaryWhenAllColumnsPresent() throws Exception {
    // given
    given(rs.getString("customer_id"))
        .willReturn(CustomerSummaryFixtures.SOME_CUSTOMER_ID.value().toString());
    given(rs.getString("name")).willReturn("Acme Corp");
    given(rs.getString("email")).willReturn("masked-customer@example.com");
    given(rs.getString("kyc")).willReturn("VERIFIED");
    given(rs.getLong("balance_micros")).willReturn(MoneyFixtures.SOME_MONEY.toMicros());
    given(rs.getString("balance_currency_code"))
        .willReturn(MoneyFixtures.SOME_MONEY.currency().name());
    given(rs.getLong("total_sent_micros")).willReturn(MoneyFixtures.SOME_MONEY.toMicros());
    given(rs.getString("total_sent_currency_code"))
        .willReturn(MoneyFixtures.SOME_MONEY.currency().name());
    given(rs.getInt("txn_count")).willReturn(42);
    given(rs.getTimestamp("joined_at"))
        .willReturn(Timestamp.from(CustomerSummaryFixtures.SOME_CUSTOMER_JOINED));
    given(rs.getString("risk_tier")).willReturn("LOW");
    var expected = CustomerSummaryFixtures.SOME_CUSTOMER_SUMMARY;

    // when
    var actual = TrinoCustomerRepository.CUSTOMER_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
