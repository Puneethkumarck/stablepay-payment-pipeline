package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Schema(description = "Dashboard statistics for a time period")
@Builder(toBuilder = true)
public record DashboardStatsDto(
    @JsonProperty("volume_24h_micros") long volume24hMicros,
    @JsonProperty("currency_code") String currencyCode,
    @JsonProperty("transaction_count") long transactionCount,
    @JsonProperty("success_rate") double successRate,
    @JsonProperty("dlq_count") long dlqCount,
    @JsonProperty("dlq_critical_count") long dlqCriticalCount,
    @JsonProperty("stuck_count") long stuckCount,
    @JsonProperty("stuck_critical_count") long stuckCriticalCount,
    @JsonProperty("period_start") Instant periodStart,
    @JsonProperty("period_end") Instant periodEnd) {

  public DashboardStatsDto {
    Objects.requireNonNull(currencyCode, "currencyCode");
    Objects.requireNonNull(periodStart, "periodStart");
    Objects.requireNonNull(periodEnd, "periodEnd");
  }
}
