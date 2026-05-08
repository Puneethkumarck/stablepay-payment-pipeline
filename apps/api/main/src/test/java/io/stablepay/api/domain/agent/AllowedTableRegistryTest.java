package io.stablepay.api.domain.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AllowedTableRegistryTest {

  private final AllowedTableRegistry registry = new AllowedTableRegistry();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "iceberg.analytics.v_payment_summary",
        "iceberg.analytics.v_daily_report",
        "iceberg.facts.fact_transactions",
        "iceberg.facts.fact_payments",
        "iceberg.agg.agg_daily_volume",
        "iceberg.agg.agg_monthly_totals",
        "ICEBERG.ANALYTICS.V_PAYMENT_SUMMARY",
        "Iceberg.Facts.Fact_Transactions"
      })
  void shouldAllowValidTables(String table) {
    // when
    var result = registry.isAllowed(table);

    // then
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "postgres.public.users",
        "iceberg.dlq.dlq_events",
        "iceberg.analytics.payments",
        "iceberg.facts.transactions",
        "iceberg.agg.volumes",
        "iceberg.raw.fact_transactions",
        "v_payment_summary"
      })
  void shouldRejectDisallowedTables(String table) {
    // when
    var result = registry.isAllowed(table);

    // then
    assertThat(result).isFalse();
  }
}
