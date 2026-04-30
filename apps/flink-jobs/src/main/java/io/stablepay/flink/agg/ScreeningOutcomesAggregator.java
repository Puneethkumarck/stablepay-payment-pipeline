package io.stablepay.flink.agg;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;

import io.stablepay.flink.model.ValidatedEvent;

public class ScreeningOutcomesAggregator
        implements AggregateFunction<ValidatedEvent, ScreeningAccumulator, RowData> {

    @Override
    public ScreeningAccumulator createAccumulator() {
        return new ScreeningAccumulator(0L, 0L, 0.0, 0L, "UNKNOWN", "UNKNOWN");
    }

    @Override
    public ScreeningAccumulator add(ValidatedEvent event, ScreeningAccumulator acc) {
        var record = event.toRecord();
        var outcome = record.get("screening_outcome");
        var provider = record.get("provider");
        var durationMs = 0L;
        var durationField = record.get("duration_ms");
        if (durationField instanceof Number n) {
            durationMs = n.longValue();
        }
        var score = 0.0;
        var scoreCount = 0L;
        var scoreField = record.get("score");
        if (scoreField instanceof Number n) {
            score = n.doubleValue();
            scoreCount = 1L;
        }

        return acc.toBuilder()
                .totalCount(acc.totalCount() + 1)
                .totalDurationMs(acc.totalDurationMs() + durationMs)
                .totalScore(acc.totalScore() + score)
                .scoreCount(acc.scoreCount() + scoreCount)
                .outcome(outcome != null ? outcome.toString() : acc.outcome())
                .provider(provider != null ? provider.toString() : acc.provider())
                .build();
    }

    @Override
    public RowData getResult(ScreeningAccumulator acc) {
        var row = new GenericRowData(6);
        row.setField(0, null);
        row.setField(1, StringData.fromString(acc.outcome()));
        row.setField(2, StringData.fromString(acc.provider()));
        row.setField(3, acc.totalCount());
        var avgDuration = acc.totalCount() > 0
                ? acc.totalDurationMs() / (double) acc.totalCount()
                : 0.0;
        row.setField(4, avgDuration);
        var avgScore = acc.scoreCount() > 0
                ? acc.totalScore() / acc.scoreCount()
                : 0.0;
        row.setField(5, avgScore);
        return row;
    }

    @Override
    public ScreeningAccumulator merge(ScreeningAccumulator a, ScreeningAccumulator b) {
        return a.toBuilder()
                .totalCount(a.totalCount() + b.totalCount())
                .totalDurationMs(a.totalDurationMs() + b.totalDurationMs())
                .totalScore(a.totalScore() + b.totalScore())
                .scoreCount(a.scoreCount() + b.scoreCount())
                .build();
    }
}
