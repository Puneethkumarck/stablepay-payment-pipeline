package io.stablepay.flink.mapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.DlqEventIds;

public final class DlqToIcebergRowMapper {

    private DlqToIcebergRowMapper() {}

    public static RowData toRowData(DlqEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(envelope.sourceTopic(), "sourceTopic");
        Objects.requireNonNull(envelope.errorClass(), "errorClass");
        Objects.requireNonNull(envelope.errorMessage(), "errorMessage");

        var row = new GenericRowData(9);

        row.setField(0, StringData.fromString(DlqEventIds.of(envelope)));
        row.setField(1, StringData.fromString(envelope.sourceTopic()));
        row.setField(2, envelope.sourcePartition());
        row.setField(3, envelope.sourceOffset());
        row.setField(4, StringData.fromString(envelope.errorClass()));
        row.setField(5, StringData.fromString(envelope.errorMessage()));
        row.setField(6, TimestampData.fromInstant(Instant.ofEpochMilli(envelope.failedAt())));
        row.setField(7, envelope.retryCount());

        var payload = envelope.originalPayloadBytes();
        row.setField(8, payload != null
                ? StringData.fromString(new String(payload, StandardCharsets.UTF_8))
                : null);

        return row;
    }
}
