package io.stablepay.api.domain.agent;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AllowedQueryShapeRegistry {

  private static final Set<String> ALLOWED_INDICES = Set.of("transactions", "dlq-events");

  private static final Set<String> ALLOWED_CALENDAR_INTERVALS =
      Set.of("minute", "hour", "day", "week", "month", "quarter", "year");

  private static final Set<String> ALLOWED_FIELDS =
      Set.of(
          "event_id",
          "customer_id",
          "account_id",
          "event_time",
          "ingest_time",
          "internal_status",
          "customer_status",
          "flow_type",
          "flow_id",
          "currency_code",
          "amount_micros",
          "error_class",
          "source_topic",
          "source_partition",
          "source_offset",
          "failed_at",
          "retry_count",
          "sink_type");

  public boolean isIndexAllowed(String index) {
    return ALLOWED_INDICES.contains(index);
  }

  public boolean isFieldAllowed(String field) {
    return ALLOWED_FIELDS.contains(field);
  }

  public boolean isCalendarIntervalAllowed(String interval) {
    return ALLOWED_CALENDAR_INTERVALS.contains(interval.toLowerCase(Locale.ROOT));
  }
}
