package io.stablepay.api.domain.agent.fixtures;

import io.stablepay.api.domain.agent.TimelineEntry;
import java.time.Instant;
import java.util.List;

public final class AgentServiceFixtures {

  public static final String SOME_REFERENCE = "ref-abc-123";
  public static final String SOME_QUERY_ID = "q-00000000-0000-0000-0000-000000000001";

  public static final String SOME_VALID_SQL =
      "SELECT * FROM iceberg.analytics.v_payment_summary WHERE currency = 'USD'";

  public static final Instant SOME_EVENT_TIME_1 = Instant.parse("2026-01-15T10:00:00Z");
  public static final Instant SOME_EVENT_TIME_2 = Instant.parse("2026-01-15T10:05:00Z");

  public static final TimelineEntry SOME_TIMELINE_ENTRY_1 =
      TimelineEntry.builder()
          .eventId("evt-aabbccdd-1111-2222-3333-444444444444")
          .source("transaction")
          .status("INITIATED")
          .detail("ON_RAMP")
          .timestamp(SOME_EVENT_TIME_1)
          .build();

  public static final TimelineEntry SOME_TIMELINE_ENTRY_2 =
      TimelineEntry.builder()
          .eventId("evt-eeff")
          .source("screening")
          .status("PASSED")
          .detail("AML_CHECK")
          .timestamp(SOME_EVENT_TIME_2)
          .build();

  public static final List<TimelineEntry> SOME_TIMELINE_ENTRIES =
      List.of(SOME_TIMELINE_ENTRY_1, SOME_TIMELINE_ENTRY_2);

  private AgentServiceFixtures() {}
}
