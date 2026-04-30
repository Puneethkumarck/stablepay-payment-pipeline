package io.stablepay.flink.agg;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;

import io.stablepay.flink.model.DlqEnvelope;

class DlqSummaryAggregator implements AggregateFunction<DlqEnvelope, DlqAccumulator, RowData> {

    static final String UNKNOWN = "UNKNOWN";

    @Override
    public DlqAccumulator createAccumulator() {
        return new DlqAccumulator(0L, 0, UNKNOWN, UNKNOWN);
    }

    @Override
    public DlqAccumulator add(DlqEnvelope envelope, DlqAccumulator acc) {
        return acc.toBuilder()
                .eventCount(acc.eventCount() + 1)
                .maxRetryCount(Math.max(acc.maxRetryCount(), envelope.retryCount()))
                .errorClass(envelope.errorClass() != null ? envelope.errorClass() : acc.errorClass())
                .sourceTopic(envelope.sourceTopic() != null ? envelope.sourceTopic() : acc.sourceTopic())
                .build();
    }

    @Override
    public RowData getResult(DlqAccumulator acc) {
        var row = new GenericRowData(6);
        row.setField(0, null);
        row.setField(1, null);
        row.setField(2, StringData.fromString(acc.errorClass()));
        row.setField(3, StringData.fromString(acc.sourceTopic()));
        row.setField(4, acc.eventCount());
        row.setField(5, acc.maxRetryCount());
        return row;
    }

    @Override
    public DlqAccumulator merge(DlqAccumulator a, DlqAccumulator b) {
        return a.toBuilder()
                .eventCount(a.eventCount() + b.eventCount())
                .maxRetryCount(Math.max(a.maxRetryCount(), b.maxRetryCount()))
                .errorClass(UNKNOWN.equals(a.errorClass()) ? b.errorClass() : a.errorClass())
                .sourceTopic(UNKNOWN.equals(a.sourceTopic()) ? b.sourceTopic() : a.sourceTopic())
                .build();
    }
}
