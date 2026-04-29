package io.stablepay.flink.mapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;

import io.stablepay.flink.model.DlqEnvelope;

public final class DlqToIcebergRowMapper {

    private DlqToIcebergRowMapper() {}

    public static RowData toRowData(DlqEnvelope envelope) {
        var row = new GenericRowData(9);

        row.setField(0, StringData.fromString(UUID.randomUUID().toString()));
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
