package io.stablepay.api.domain.model.fixtures;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.DashboardStats;
import java.time.Instant;

public final class DashboardStatsFixtures {

  public static final Instant SOME_PERIOD_START = Instant.parse("2026-05-01T00:00:00Z");
  public static final Instant SOME_PERIOD_END = Instant.parse("2026-05-02T00:00:00Z");

  public static final DashboardStats SOME_DASHBOARD_STATS =
      DashboardStats.builder()
          .volume24hMicros(1_500_000_000L)
          .currencyCode(CurrencyCode.USD)
          .transactionCount(42L)
          .successRate(0.95)
          .dlqCount(3L)
          .dlqCriticalCount(1L)
          .stuckCount(2L)
          .stuckCriticalCount(0L)
          .periodStart(SOME_PERIOD_START)
          .periodEnd(SOME_PERIOD_END)
          .build();

  private DashboardStatsFixtures() {}

  public static DashboardStats.DashboardStatsBuilder someDashboardStats() {
    return SOME_DASHBOARD_STATS.toBuilder();
  }
}
