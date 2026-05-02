package io.stablepay.api.domain.model;

import com.neovisionaries.i18n.CurrencyCode;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record DashboardStats(
    long volume24hMicros,
    CurrencyCode currencyCode,
    long transactionCount,
    double successRate,
    long dlqCount,
    long dlqCriticalCount,
    long stuckCount,
    long stuckCriticalCount,
    Instant periodStart,
    Instant periodEnd) {

  public DashboardStats {
    Objects.requireNonNull(currencyCode, "currencyCode");
    Objects.requireNonNull(periodStart, "periodStart");
    Objects.requireNonNull(periodEnd, "periodEnd");
  }
}
