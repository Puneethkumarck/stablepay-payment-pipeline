package io.stablepay.api.domain.model.fixtures;

import io.stablepay.api.domain.model.DlqSummary;
import java.util.Map;

public final class DlqSummaryFixtures {

  public static final Map<String, Long> SOME_COUNTS_BY_ERROR_CLASS =
      Map.of(
          "DeserializationException", 5L,
          "TimeoutException", 3L,
          "ValidationException", 2L);

  public static final DlqSummary SOME_DLQ_SUMMARY =
      DlqSummary.builder().countsByErrorClass(SOME_COUNTS_BY_ERROR_CLASS).totalCount(10L).build();

  private DlqSummaryFixtures() {}

  public static DlqSummary.DlqSummaryBuilder someDlqSummary() {
    return SOME_DLQ_SUMMARY.toBuilder();
  }
}
