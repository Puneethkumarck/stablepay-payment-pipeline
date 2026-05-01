package io.stablepay.flink.agg;

import io.stablepay.flink.model.ValidatedEvent;
import java.util.Set;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;

class SuccessRateAggregator
    implements AggregateFunction<ValidatedEvent, SuccessRateAccumulator, RowData> {

  static final String UNKNOWN = "UNKNOWN";

  private static final Set<String> COMPLETED_STATUSES = Set.of("COMPLETED");
  private static final Set<String> FAILED_STATUSES =
      Set.of("FAILED", "CANCELLED", "REJECTED", "CONFISCATED");

  @Override
  public SuccessRateAccumulator createAccumulator() {
    return new SuccessRateAccumulator(0L, 0L, 0L, UNKNOWN);
  }

  @Override
  public SuccessRateAccumulator add(ValidatedEvent event, SuccessRateAccumulator acc) {
    var record = event.toRecord();
    var status = record.get("internal_status");
    var statusStr = status != null ? status.toString() : "";
    var flowType = deriveFlowType(event.topic());

    var completed = COMPLETED_STATUSES.contains(statusStr) ? 1L : 0L;
    var failed = FAILED_STATUSES.contains(statusStr) ? 1L : 0L;

    return acc.toBuilder()
        .totalCount(acc.totalCount() + 1)
        .completedCount(acc.completedCount() + completed)
        .failedCount(acc.failedCount() + failed)
        .flowType(flowType)
        .build();
  }

  @Override
  public RowData getResult(SuccessRateAccumulator acc) {
    var row = new GenericRowData(7);
    row.setField(0, null);
    row.setField(1, null);
    row.setField(2, StringData.fromString(acc.flowType()));
    row.setField(3, acc.totalCount());
    row.setField(4, acc.completedCount());
    row.setField(5, acc.failedCount());
    var successRate = acc.totalCount() > 0 ? acc.completedCount() / (double) acc.totalCount() : 0.0;
    row.setField(6, successRate);
    return row;
  }

  @Override
  public SuccessRateAccumulator merge(SuccessRateAccumulator a, SuccessRateAccumulator b) {
    return a.toBuilder()
        .totalCount(a.totalCount() + b.totalCount())
        .completedCount(a.completedCount() + b.completedCount())
        .failedCount(a.failedCount() + b.failedCount())
        .flowType(UNKNOWN.equals(a.flowType()) ? b.flowType() : a.flowType())
        .build();
  }

  private static String deriveFlowType(String topic) {
    if (topic.contains("crypto") || topic.contains("chain")) return "CRYPTO";
    if (topic.contains("fiat")) return "FIAT";
    return "MIXED";
  }
}
