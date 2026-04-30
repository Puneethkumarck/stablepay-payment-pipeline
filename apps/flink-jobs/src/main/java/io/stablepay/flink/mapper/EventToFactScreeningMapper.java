package io.stablepay.flink.mapper;

import java.time.Instant;

import org.apache.avro.generic.GenericRecord;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;

import io.stablepay.flink.model.ValidatedEvent;

public final class EventToFactScreeningMapper {

    private EventToFactScreeningMapper() {}

    public static RowData toRowData(ValidatedEvent event) {
        var record = event.toRecord();
        var row = new GenericRowData(10);

        row.setField(0, StringData.fromString(event.eventId()));
        row.setField(1, stringDataOrNull(stringVal(record.get("screening_id"))));
        row.setField(2, stringDataOrNull(PiiMasker.mask(stringVal(record.get("customer_id")))));
        row.setField(3, stringDataOrNull(extractTransactionReference(record)));
        row.setField(4, stringDataOrNull(stringVal(record.get("outcome"))));
        row.setField(5, stringDataOrNull(stringVal(record.get("provider"))));
        row.setField(6, stringDataOrNull(stringVal(record.get("rule_triggered"))));

        var score = record.get("score");
        row.setField(7, score instanceof Number n ? n.doubleValue() : null);

        row.setField(8, TimestampData.fromInstant(Instant.ofEpochMilli(event.eventTimeMillis())));

        var durationMs = record.get("duration_ms");
        row.setField(9, durationMs instanceof Number n ? n.longValue() : null);

        return row;
    }

    private static String extractTransactionReference(GenericRecord record) {
        var ref = record.get("transaction_reference");
        if (ref != null) return ref.toString();
        var payoutRef = record.get("payout_reference");
        return payoutRef != null ? payoutRef.toString() : null;
    }

    private static StringData stringDataOrNull(String value) {
        return value != null ? StringData.fromString(value) : null;
    }

    private static String stringVal(Object value) {
        return value != null ? value.toString() : null;
    }
}
