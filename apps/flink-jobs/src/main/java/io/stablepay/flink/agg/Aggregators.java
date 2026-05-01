package io.stablepay.flink.agg;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.ValidatedEvent;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.table.data.RowData;

public final class Aggregators {

  private Aggregators() {}

  public static AggregateFunction<ValidatedEvent, ?, RowData> volume() {
    return new VolumeAggregator();
  }

  public static AggregateFunction<ValidatedEvent, ?, RowData> successRate() {
    return new SuccessRateAggregator();
  }

  public static AggregateFunction<ValidatedEvent, ?, RowData> screeningOutcomes() {
    return new ScreeningOutcomesAggregator();
  }

  public static AggregateFunction<DlqEnvelope, ?, RowData> dlqSummary() {
    return new DlqSummaryAggregator();
  }

  public static <K> ProcessWindowFunction<RowData, RowData, K, TimeWindow> windowTimestamptz(
      int startFieldIndex, int endFieldIndex) {
    return WindowTimestampEmitter.timestamptz(startFieldIndex, endFieldIndex);
  }

  public static <K> ProcessWindowFunction<RowData, RowData, K, TimeWindow> windowDate(
      int dateFieldIndex) {
    return WindowTimestampEmitter.date(dateFieldIndex);
  }
}
