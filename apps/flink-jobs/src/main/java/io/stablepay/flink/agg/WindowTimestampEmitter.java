package io.stablepay.flink.agg;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.util.Collector;

final class WindowTimestampEmitter {

    private WindowTimestampEmitter() {}

    static <K> ProcessWindowFunction<RowData, RowData, K, TimeWindow> timestamptz(
            int startFieldIndex, int endFieldIndex) {
        return new ProcessWindowFunction<>() {
            @Override
            public void process(K key, Context context, Iterable<RowData> elements,
                    Collector<RowData> out) {
                var window = context.window();
                for (var element : elements) {
                    var row = (GenericRowData) element;
                    row.setField(startFieldIndex, TimestampData.fromInstant(
                            Instant.ofEpochMilli(window.getStart())));
                    row.setField(endFieldIndex, TimestampData.fromInstant(
                            Instant.ofEpochMilli(window.getEnd())));
                    out.collect(row);
                }
            }
        };
    }

    static <K> ProcessWindowFunction<RowData, RowData, K, TimeWindow> date(int dateFieldIndex) {
        return new ProcessWindowFunction<>() {
            @Override
            public void process(K key, Context context, Iterable<RowData> elements,
                    Collector<RowData> out) {
                var epochDay = (int) LocalDate.ofInstant(
                        Instant.ofEpochMilli(context.window().getStart()), ZoneOffset.UTC).toEpochDay();
                for (var element : elements) {
                    var row = (GenericRowData) element;
                    row.setField(dateFieldIndex, epochDay);
                    out.collect(row);
                }
            }
        };
    }
}
